/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.betsy.music.utils.localizedStreamingTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betsy.music.ui.component.PlayingIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.ArtistItem
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.audius.compat.models.YTItem
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.StreamingGridItem
import com.betsy.music.ui.component.shimmer.GridItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.ui.component.shimmer.TextPlaceholder
import com.betsy.music.ui.menu.StreamingAlbumMenu
import com.betsy.music.ui.menu.StreamingArtistMenu
import com.betsy.music.ui.menu.StreamingPlaylistMenu
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.HomeViewModel
import kotlin.math.min



private fun isTrendingSection22(title: String): Boolean =
    listOf("xito", "hit", "trend", "popular", "top songs", "canciones más")
        .any { title.contains(it, ignoreCase = true) }

private fun isSectionExcluded22(title: String): Boolean =
    listOf("instrumento", "instrumental", "clásic", "clasic", "esencial", "actuaci", "directo", "live", "en vivo")
        .any { title.contains(it, ignoreCase = true) }





private data class DeviceSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: android.net.Uri,
    val albumArtUri: android.net.Uri?,
    val lastModified: Long = 0L,
)

private fun DeviceSong.toLocalMediaItem(): androidx.media3.common.MediaItem {
    val mediaId = "device_$id"
    val musicMeta = com.betsy.music.models.MediaMetadata(
        id = mediaId,
        title = title,
        artists = if (artist.isNotBlank())
            listOf(com.betsy.music.models.MediaMetadata.Artist(id = null, name = artist))
        else emptyList(),
        album = album.takeIf { it.isNotBlank() }
            ?.let { com.betsy.music.models.MediaMetadata.Album(id = "", title = it) },
        duration = (duration / 1000).toInt(),
        thumbnailUrl = albumArtUri?.toString(),
    )
    val extras = android.os.Bundle().apply {
        putString("mediaId", mediaId)
        putBoolean("isLocal", true)
    }
    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(uri)
        .setCustomCacheKey(mediaId)
        .setRequestMetadata(
            androidx.media3.common.MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri)
                .setExtras(extras)
                .build()
        )
        .setTag(musicMeta)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri)
                .build()
        ).build()
}



@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: run {

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF3b9cac))
        }
        return
    }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val homePage by viewModel.homePage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val iaAlbums by viewModel.iaAlbums.collectAsState()
    val localTrendingSongs by viewModel.localTrendingSongs.collectAsState()
    val madeForYouPlaylists by viewModel.madeForYouPlaylists.collectAsState()

    val isGuest by viewModel.isGuest.collectAsState()
    val guestShortId by viewModel.guestShortId.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()


    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val isStreamingMode by rememberPreference(StreamingModeKey, defaultValue = true)
    val showStreamingFirst = isStreamingMode

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }


    var cachedTrendingSection by remember { mutableStateOf(homePage?.sections?.find { isTrendingSection22(it.title) }) }
    var cachedAlbumSections by remember { mutableStateOf(homePage?.sections
        ?.filterNot { isTrendingSection22(it.title) || isSectionExcluded22(it.title) }
        ?.filter { it.items.isNotEmpty() } ?: emptyList()) }
    var cachedAllSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }

    LaunchedEffect(homePage) {
        if (homePage != null) {
            val sections = homePage!!.sections
            cachedTrendingSection = sections.find { isTrendingSection22(it.title) }
            cachedAlbumSections = sections
                .filterNot { isTrendingSection22(it.title) || isSectionExcluded22(it.title) }
                .filter { it.items.isNotEmpty() }
            val trendingIds = cachedTrendingSection?.items?.map { it.id }?.toSet() ?: emptySet()
            cachedAllSongs = sections
                .flatMap { it.items }
                .filterIsInstance<SongItem>()
                .filter { it.id !in trendingIds }
        }
    }

    val trendingSection = cachedTrendingSection
    val albumSections = cachedAlbumSections
    val allSongs = cachedAllSongs

    val strTrendingNow = stringResource(R.string.section_trending_now)
    val strTrendingSubtitle = stringResource(R.string.section_trending_subtitle)
    val strLoadingTopHits = stringResource(R.string.loading_top_hits)
    val strErrorConnection = stringResource(R.string.error_check_connection)
    val strMadeForYou = stringResource(R.string.section_made_for_you)
    val strAllSongs = stringResource(R.string.section_all_songs)
    val strModeStreaming = stringResource(R.string.mode_streaming)
    val strModeLocal = stringResource(R.string.mode_local)


    val context = LocalContext.current

    var deviceSongs by remember { mutableStateOf<List<DeviceSong>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var permissionAsked by remember { mutableStateOf(false) }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        val check = context.checkSelfPermission(audioPermission)
        hasPermission = (check == PackageManager.PERMISSION_GRANTED)
        if (!hasPermission && !permissionAsked) {
            permissionAsked = true
            permissionLauncher.launch(audioPermission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            deviceSongs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val list = mutableListOf<DeviceSong>()
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"
                val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
                context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val albumId = cursor.getLong(albumIdCol)
                        list.add(DeviceSong(
                            id = id,
                            title = cursor.getString(titleCol) ?: context.getString(R.string.unknown),
                            artist = cursor.getString(artistCol) ?: context.getString(R.string.unknown_artist),
                            album = cursor.getString(albumCol) ?: context.getString(R.string.unknown_album),
                            duration = cursor.getLong(durCol),
                            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            albumArtUri = ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), albumId),
                            lastModified = cursor.getLong(modCol),
                        ))
                    }
                }
                list
            }
        }
    }

    val localAllSongs = deviceSongs
    val localRecentSongs = remember(deviceSongs) { deviceSongs.take(10) }
    val localTrendingLocal = remember(deviceSongs) { deviceSongs.shuffled().take(8) }
    val localForgotten = remember(deviceSongs) { deviceSongs.drop(20).take(16) }
    val forgottenFavoritesGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()


    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        StreamingGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier.combinedClickable(
                onClick = {
                    when (item) {
                        is SongItem -> playerConnection.playQueue(StreamingQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                        is AlbumItem -> navController.navigate("album/${item.id}")
                        is ArtistItem -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem -> StreamingSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                            is AlbumItem -> StreamingAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                            is ArtistItem -> StreamingArtistMenu(artist = item, onDismiss = menuState::dismiss)
                            is PlaylistItem -> StreamingPlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                        }
                    }
                }
            )
        )
    }





    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {


            item {
                Spacer(modifier = Modifier.statusBarsPadding())

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {

                        Text(
                            text = if (isStreamingMode) strModeStreaming else strModeLocal,
                            fontSize = 14.sp,
                            color = accentColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }




            if (showStreamingFirst) {


                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(strTrendingNow, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor)
                        Text(
                            text = trendingSection?.title ?: strTrendingSubtitle,
                            fontSize = 12.sp,
                            color = adaptiveColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )
                    }
                }
                item {
                    val displaySongs = trendingSection?.items?.filterIsInstance<SongItem>()
                        ?.ifEmpty { trendingSongs } ?: trendingSongs
                    when {
                        displaySongs.isEmpty() && isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(strLoadingTopHits, fontSize = 14.sp, color = adaptiveColor.copy(alpha = 0.6f))
                                }
                            }
                        }
                        displaySongs.isNotEmpty() -> {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(items = displaySongs.distinctBy { it.id }.take(8), key = { it.id }) { songItem ->
                                    TrendingStreamingCard22(
                                        songItem = songItem,
                                        isActive = songItem.id == mediaMetadata?.id,
                                        isPlaying = isPlaying && songItem.id == mediaMetadata?.id,
                                        accentColor = accentColor,
                                        onClick = {
                                            if (songItem.id == mediaMetadata?.id)
                                                playerConnection.player.togglePlayPause()
                                            else playerConnection.playQueue(
                                                StreamingQueue(songItem.endpoint ?: WatchEndpoint(videoId = songItem.id), songItem.toMediaMetadata())
                                            )
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                StreamingSongMenu(song = songItem, navController = navController, onDismiss = menuState::dismiss)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(strErrorConnection, fontSize = 14.sp, color = adaptiveColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                }


                if (madeForYouPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = strMadeForYou,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(items = madeForYouPlaylists.distinctBy { it.id }, key = { it.id }) { playlist ->
                                MadeForYouCard22(
                                    title = playlist.title,
                                    onClick = { navController.navigate("online_playlist/${playlist.id}") },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            StreamingPlaylistMenu(playlist = playlist, coroutineScope = scope, onDismiss = menuState::dismiss)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else if (isLoading) {
                    item {
                        ShimmerHost {
                            TextPlaceholder(
                                height = 28.dp,
                                modifier = Modifier.padding(horizontal = 16.dp).width(160.dp).padding(bottom = 12.dp)
                            )
                            LazyRow { items(4) { GridItemPlaceHolder() } }
                        }
                    }
                }



                albumSections.forEach { section ->
                    item {
                        Text(
                            text = localizedStreamingTitle(section.title),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(section.items) { item ->
                                HorizontalCardItem(
                                    item = item,
                                    isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                    isPlaying = isPlaying && item.id == mediaMetadata?.id,
                                    accentColor = accentColor,
                                    onClick = {
                                        when (item) {
                                            is SongItem -> playerConnection.playQueue(StreamingQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            when (item) {
                                                is SongItem -> StreamingSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                                is AlbumItem -> StreamingAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                                is ArtistItem -> StreamingArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                                is PlaylistItem -> StreamingPlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }


                if (iaAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.ia_source_ia),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(items = iaAlbums.distinctBy { it.id }, key = { it.id }) { album ->
                                HorizontalCardItem(
                                    item = album,
                                    isActive = false,
                                    accentColor = accentColor,
                                    onClick = {
                                        navController.navigate(
                                            "ia_item/${android.net.Uri.encode(com.betsy.music.utils.iaIdentifierOf(album.id))}"
                                        )
                                    },
                                    onLongClick = { }
                                )
                            }
                        }
                    }
                }


                if (allSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = strAllSongs,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)
                        )
                    }
                    items(allSongs.distinctBy { it.id }, key = { it.id }) { songItem ->
                        AllSongListItem22(
                            songItem = songItem,
                            isActive = songItem.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = {
                                if (songItem.id == mediaMetadata?.id)
                                    playerConnection.player.togglePlayPause()
                                else playerConnection.playQueue(
                                    StreamingQueue(songItem.endpoint ?: WatchEndpoint(videoId = songItem.id), songItem.toMediaMetadata())
                                )
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    StreamingSongMenu(song = songItem, navController = navController, onDismiss = menuState::dismiss)
                                }
                            }
                        )
                    }
                } else if (isLoading) {
                    item {
                        ShimmerHost {
                            TextPlaceholder(height = 36.dp, modifier = Modifier.padding(12.dp).width(250.dp))
                            LazyRow { items(4) { GridItemPlaceHolder() } }
                        }
                    }
                }




            } else {

                if (!hasPermission) {
                    item {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MusicNote, null,
                                tint     = accentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.permission_required), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = adaptiveColor)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.local_permission_message),
                                fontSize  = 14.sp,
                                color     = adaptiveColor.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            androidx.compose.material3.Button(
                                onClick = { permissionLauncher.launch(audioPermission) },
                                colors  = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape   = RoundedCornerShape(12.dp)
                            ) { Text(stringResource(R.string.grant_permission)) }
                        }
                    }
                } else {


                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(strTrendingNow, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor)
                            Text(
                                stringResource(R.string.section_trending_local_subtitle),
                                fontSize = 12.sp, color = adaptiveColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )
                        }
                    }
                    item {
                        when {
                            localTrendingLocal.isEmpty() && isLoading -> {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(48.dp))
                                }
                            }
                            localTrendingLocal.isNotEmpty() -> {
                                LazyRow(
                                    contentPadding        = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(items = localTrendingLocal, key = { it.id }) { song ->
                                        DeviceSongCard(
                                            title       = song.title,
                                            artist      = song.artist,
                                            artUri      = song.albumArtUri,
                                            isActive    = "device_${song.id}" == mediaMetadata?.id,
                                            isPlaying   = isPlaying && "device_${song.id}" == mediaMetadata?.id,
                                            accentColor = accentColor,
                                            onClick     = {
                                                val idx   = localAllSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                                val items = localAllSongs.map { s -> s.toLocalMediaItem() }
                                                playerConnection.playQueue(ListQueue(title = context.getString(R.string.local_music), items = items, startIndex = idx))
                                            }
                                        )
                                    }
                                }
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.no_music_on_device), fontSize = 14.sp, color = adaptiveColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }


                    if (localRecentSongs.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.recently_added),
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp)
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding        = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(items = localRecentSongs, key = { it.id }) { song ->
                                    RecentlyAddedCard(
                                        title       = song.title,
                                        artist      = song.artist,
                                        artUri      = song.albumArtUri,
                                        accentColor = accentColor,
                                        onClick     = {
                                            val idx   = localAllSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                            val items = localAllSongs.map { s -> s.toLocalMediaItem() }
                                            playerConnection.playQueue(ListQueue(title = context.getString(R.string.local_music), items = items, startIndex = idx))
                                        }
                                    )
                                }
                            }
                        }
                    }


                    if (localAllSongs.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.your_music_count, localAllSongs.size),
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp)
                            )
                        }
                        items(items = localAllSongs.take(30), key = { it.id }) { song ->
                            val isActive = "device_${song.id}" == mediaMetadata?.id
                            DeviceSongListItem(
                                title       = song.title,
                                artist      = song.artist,
                                duration    = song.duration,
                                artUri      = song.albumArtUri,
                                isActive    = isActive,
                                isPlaying   = isActive && isPlaying,
                                accentColor = accentColor,
                                modifier    = Modifier.padding(horizontal = 16.dp),
                                onClick     = {
                                    if (isActive) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        val idx   = localAllSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                        val items = localAllSongs.map { s -> s.toLocalMediaItem() }
                                        playerConnection.playQueue(ListQueue(title = context.getString(R.string.local_music), items = items, startIndex = idx))
                                    }
                                }
                            )
                        }
                        if (localAllSongs.size > 30) {
                            item {
                                androidx.compose.material3.TextButton(
                                    onClick  = { navController.navigate("local_songs") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.see_more_songs, localAllSongs.size - 30), color = accentColor)
                                }
                            }
                        }
                    } else {
                        item {
                            Column(
                                modifier            = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.MusicNote, null, tint = adaptiveColor.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(stringResource(R.string.no_music_on_device), fontSize = 16.sp, color = adaptiveColor.copy(alpha = 0.6f))
                            }
                        }
                    }


                    if (localForgotten.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.forgotten_favorites),
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp)
                            )
                        }
                        item {
                            val rows = min(4, localForgotten.size)
                            LazyHorizontalGrid(
                                state          = forgottenFavoritesGridState,
                                rows           = GridCells.Fixed(rows),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier       = Modifier.fillMaxWidth().height((72 * rows).dp)
                            ) {
                                items(items = localForgotten, key = { it.id }) { song ->
                                    val isActive = "device_${song.id}" == mediaMetadata?.id
                                    DeviceSongGridItem(
                                        title       = song.title,
                                        artist      = song.artist,
                                        artUri      = song.albumArtUri,
                                        isActive    = isActive,
                                        isPlaying   = isActive && isPlaying,
                                        accentColor = accentColor,
                                        modifier    = Modifier.width(280.dp),
                                        onClick     = {
                                            if (isActive) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                val idx   = localAllSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                                val items = localAllSongs.map { s -> s.toLocalMediaItem() }
                                                playerConnection.playQueue(ListQueue(title = context.getString(R.string.forgotten_favorites), items = items, startIndex = idx))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
        )
    }
}





@Composable
private fun HomeEqualizerOverlay(
    accentColor: Color,
    cornerRadius: Dp = 12.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        PlayingIndicator(color = accentColor, modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrendingStreamingCard22(
    songItem: SongItem,
    isActive: Boolean,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    accentColor: Color = Color(0xFF3b9cac),
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isActive) 2.dp else 0.dp,
                    color = if (isActive) accentColor else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            if (songItem.thumbnail != null) {
                AsyncImage(
                    model = songItem.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1A4A5A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(songItem.title.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }
            if (isActive && isPlaying) {
                HomeEqualizerOverlay(accentColor = accentColor, cornerRadius = 12.dp)
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd).padding(8.dp).size(32.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = if (isActive) accentColor else Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            songItem.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (isActive) accentColor else adaptiveColor, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            songItem.artists.joinToString { it.name }, fontSize = 12.sp,
            color = adaptiveColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MadeForYouCard22(
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val gradient = when {
        title.contains("night", ignoreCase = true) || title.contains("noche", ignoreCase = true) ->
            Brush.verticalGradient(listOf(Color(0xFF1A3A4A), Color(0xFF051E26)))
        title.contains("mix", ignoreCase = true) || title.contains("weekly", ignoreCase = true) ->
            Brush.verticalGradient(listOf(Color(0xFF4B3860), Color(0xFF507898)))
        title.contains("top", ignoreCase = true) || title.contains("hits", ignoreCase = true) ->
            Brush.verticalGradient(listOf(Color(0xFF7B2D8B), Color(0xFF4A1A5A)))
        else -> Brush.verticalGradient(listOf(Color(0xFF2A5A6A), Color(0xFF1A3A4A)))
    }
    Box(
        modifier = Modifier
            .width(160.dp).height(160.dp)
            .clip(RoundedCornerShape(16.dp)).background(gradient)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = title,
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllSongListItem22(
    songItem: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF3b9cac),
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) accentColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (songItem.thumbnail != null) {
                AsyncImage(
                    model = songItem.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1A4A5A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(songItem.title.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            }
            if (isActive && isPlaying) {
                HomeEqualizerOverlay(accentColor = accentColor, cornerRadius = 8.dp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                songItem.title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = if (isActive) accentColor else adaptiveColor, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                songItem.artists.joinToString { it.name }, fontSize = 14.sp,
                color = adaptiveColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Default.PlayArrow, null,
            tint = accentColor.copy(alpha = if (isActive) 1f else 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalCardItem(
    item: YTItem,
    isActive: Boolean,
    isPlaying: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(6.dp)
    ) {

        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (isActive) 2.dp else 0.dp,
                    color = if (isActive) accentColor else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            val thumbnail = when (item) {
                is SongItem -> item.thumbnail
                is AlbumItem -> item.thumbnail
                is ArtistItem -> item.thumbnail
                is PlaylistItem -> item.thumbnail
                else -> null
            }
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1A4A5A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (item) {
                            is SongItem -> item.title.take(1).uppercase()
                            is AlbumItem -> item.title.take(1).uppercase()
                            is ArtistItem -> item.title.take(1).uppercase()
                            is PlaylistItem -> item.title.take(1).uppercase()
                            else -> "?"
                        },
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            if (item is SongItem) {
                if (isActive && isPlaying) {
                    HomeEqualizerOverlay(accentColor = accentColor, cornerRadius = 12.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd).padding(8.dp).size(32.dp)
                            .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow, null,
                            tint = if (isActive) accentColor else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (item) {
                is SongItem -> item.title
                is AlbumItem -> item.title
                is ArtistItem -> item.title
                is PlaylistItem -> item.title
                else -> ""
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) accentColor else adaptiveColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val subtitle = when (item) {
            is SongItem -> item.artists.joinToString { it.name }
            is AlbumItem -> item.artists?.joinToString { it.name } ?: ""
            is ArtistItem -> stringResource(R.string.artist_label)
            is PlaylistItem -> item.author?.name ?: stringResource(R.string.context_playlist)
            else -> ""
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = adaptiveColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceSongCard(
    title: String,
    artist: String,
    artUri: Uri?,
    isActive: Boolean,
    isPlaying: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A3A4A))
                .border(
                    width = if (isActive) 2.dp else 0.dp,
                    color = if (isActive) accentColor else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artUri != null) {
                AsyncImage(model = artUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(title.take(1).uppercase(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            if (isActive && isPlaying) {
                HomeEqualizerOverlay(accentColor = accentColor, cornerRadius = 12.dp)
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd).padding(8.dp).size(32.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = if (isActive) accentColor else Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isActive) accentColor else adaptiveColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artist, fontSize = 12.sp, color = adaptiveColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentlyAddedCard(
    title: String,
    artist: String,
    artUri: Uri?,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Box(
        modifier = Modifier
            .width(160.dp).height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A5A6A), Color(0xFF1A3A4A))))
            .combinedClickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        if (artUri != null) {
            AsyncImage(model = artUri, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.4f)
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = adaptiveColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, fontSize = 11.sp, color = adaptiveColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceSongListItem(
    title: String,
    artist: String,
    duration: Long,
    artUri: Uri?,
    isActive: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) accentColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A4A5A)),
            contentAlignment = Alignment.Center
        ) {
            if (artUri != null) {
                AsyncImage(artUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(title.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            if (isActive && isPlaying) {
                HomeEqualizerOverlay(accentColor = accentColor, cornerRadius = 8.dp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isActive) accentColor else adaptiveColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, fontSize = 12.sp, color = adaptiveColor.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        val dur = run { val s = (duration / 1000).toInt(); "%02d:%02d".format(s / 60, s % 60) }
        Text(dur, fontSize = 11.sp, color = adaptiveColor.copy(alpha = 0.4f))
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = accentColor.copy(alpha = if (isActive) 1f else 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceSongGridItem(
    title: String,
    artist: String,
    artUri: Uri?,
    isActive: Boolean,
    isPlaying: Boolean = false,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) accentColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A3A4A)),
            contentAlignment = Alignment.Center
        ) {
            if (artUri != null) {
                AsyncImage(artUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(title.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
            if (isActive && isPlaying) {
                HomeEqualizerOverlay(accentColor = accentColor, cornerRadius = 8.dp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isActive) accentColor else adaptiveColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, fontSize = 11.sp, color = adaptiveColor.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}