/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import com.betsy.music.ui.theme.rememberAdaptivePanelColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalDownloadUtil
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.ListItemHeight
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.SongSortDescendingKey
import com.betsy.music.constants.SongSortType
import com.betsy.music.constants.SongSortTypeKey
import com.betsy.music.constants.YtmSyncKey
import com.betsy.music.db.entities.Song
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.playback.ExoDownloadService
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.ui.component.DefaultDialog
import com.betsy.music.ui.component.EmptyPlaceholder
import com.betsy.music.ui.component.IconButton
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.SongListItem
import com.betsy.music.ui.component.SortHeader
import com.betsy.music.ui.component.VerticalFastScroller
import com.betsy.music.ui.menu.SelectionSongMenu
import com.betsy.music.ui.menu.SongMenu
import com.betsy.music.ui.utils.ItemWrapper
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.utils.makeTimeString
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.AutoPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

enum class PlaylistType {
    LIKE, DOWNLOAD, UPLOADED, OTHER
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist = when (viewModel.playlist) {
        "liked" -> stringResource(R.string.liked)
        "uploaded" -> stringResource(R.string.uploaded_playlist)
        else -> stringResource(R.string.offline)
    }

    val songs by viewModel.likedSongs.collectAsState(null)

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    val playlistId = viewModel.playlist
    val playlistType = when (playlistId) {
        "liked" -> PlaylistType.LIKE
        "downloaded" -> PlaylistType.DOWNLOAD
        "uploaded" -> PlaylistType.UPLOADED
        else -> PlaylistType.OTHER
    }

    val wrappedSongs = remember { mutableStateListOf<ItemWrapper<Song>>() }
    LaunchedEffect(songs) {
        if (songs == null) return@LaunchedEffect
        val wrappers = withContext(Dispatchers.Default) {
            songs!!.map { item -> ItemWrapper(item) }
        }
        wrappedSongs.clear()
        wrappedSongs.addAll(wrappers)
    }

    var selection by remember { mutableStateOf(false) }

    if (isSearching) {
        BackHandler {
            isSearching = false
            searchQuery = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }


    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    
    LaunchedEffect(Unit) {
        if (ytmSync) {
            launch(Dispatchers.IO) {
                delay(300) 
                if (playlistType == PlaylistType.LIKE) viewModel.syncLikedSongs()
            }
        }
    }

    
    LaunchedEffect(songs) {
        if (songs?.isEmpty() == true) return@LaunchedEffect

        delay(500) 

        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) {
                    Download.STATE_COMPLETED
                } else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val filteredSongs = remember { mutableStateListOf<ItemWrapper<Song>>() }
    LaunchedEffect(wrappedSongs.size, searchQuery.text) {
        val result = withContext(Dispatchers.Default) {
            if (searchQuery.text.isEmpty()) wrappedSongs.toList()
            else wrappedSongs.filter { wrapper ->
                val song = wrapper.item
                song.song.title.contains(searchQuery.text, true) ||
                        song.artists.any { it.name.contains(searchQuery.text, true) }
            }
        }
        filteredSongs.clear()
        filteredSongs.addAll(result)
    }

    val state = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset < 200
        }
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    var topBarHeight by remember { mutableStateOf(56.dp) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            state = state,
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current
                    .only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom)
                    .asPaddingValues().calculateBottomPadding()
                
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight)
        ) {
            if (songs != null) {
                if (songs!!.isEmpty()) {
                    item(key = "empty_placeholder") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                

                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    Text(
                                        text = playlist,
                                        color = rememberAdaptiveOnColor(),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = buildString {
                                            append(
                                                pluralStringResource(
                                                    R.plurals.n_song,
                                                    songs!!.size,
                                                    songs!!.size
                                                )
                                            )
                                            append(" • ${makeTimeString(likeLength * 1000L)}")
                                        },
                                        color = accentColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .border(
                                                1.dp,
                                                accentColor.copy(alpha = 0.15f),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (songs!!.isNotEmpty() && songs!![0].song.thumbnailUrl != null) {
                                            AsyncImage(
                                                model = songs!![0].song.thumbnailUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(16.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(
                                                    when (playlistType) {
                                                        PlaylistType.LIKE -> R.drawable.favorite
                                                        PlaylistType.DOWNLOAD -> R.drawable.offline
                                                        else -> R.drawable.queue_music
                                                    }
                                                ),
                                                contentDescription = null,
                                                tint = when (playlistType) {
                                                    PlaylistType.LIKE -> Color(0xFFFF006E)
                                                    else -> accentColor
                                                },
                                                modifier = Modifier.size(72.dp),
                                            )
                                        }

                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        listOf(
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.4f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist,
                                                    items = songs!!.map { it.toSafeMediaItem() })
                                            )
                                        },
                                        modifier = Modifier.weight(1.4f).height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = accentColor
                                        ),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.play),
                                            null,
                                            tint = Color(0xFF071520),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.play),
                                            color =  Color(0xFF071520),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlist,
                                                    items = songs!!.shuffled()
                                                        .map { it.toSafeMediaItem() })
                                            )
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = accentColor.copy(alpha = 0.12f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            accentColor.copy(alpha = 0.35f)
                                        ),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.shuffle),
                                            null,
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.shuffle),
                                            color = accentColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                                    songs!!.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.song.id,
                                                            false
                                                        )
                                                    }
                                                }

                                                else -> {
                                                    songs!!.forEach { song ->
                                                        val downloadRequest = DownloadRequest
                                                            .Builder(
                                                                song.song.id,
                                                                song.song.id.toUri()
                                                            )
                                                            .setCustomCacheKey(song.song.id)
                                                            .setData(song.song.title.toByteArray())
                                                            .build()
                                                        DownloadService.sendAddDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            downloadRequest,
                                                            false
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = accentColor.copy(alpha = 0.12f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            accentColor.copy(alpha = 0.35f)
                                        ),
                                    ) {
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> Icon(
                                                painterResource(R.drawable.offline),
                                                null,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )

                                            Download.STATE_DOWNLOADING -> CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp),
                                                color = accentColor
                                            )

                                            else -> Icon(
                                                painterResource(R.drawable.download),
                                                null,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = when (downloadState) {
                                                Download.STATE_COMPLETED -> stringResource(R.string.remove_download)
                                                Download.STATE_DOWNLOADING -> stringResource(R.string.downloading)
                                                else -> stringResource(R.string.save)
                                            },
                                            color = accentColor,
                                            fontSize = 13.sp,
                                        )
                                    }

                                    
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            menuState.show {
                                                SelectionSongMenu(
                                                    songSelection = songs!!.toList(),
                                                    onDismiss = menuState::dismiss,
                                                    clearAction = {},
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = accentColor.copy(alpha = 0.12f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            accentColor.copy(alpha = 0.35f)
                                        ),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.more_vert),
                                            null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            stringResource(R.string.more),
                                            color = accentColor,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                androidx.compose.material3.HorizontalDivider(
                                    color = rememberAdaptivePanelColor(0.08f),
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                                Spacer(Modifier.height(8.dp))

                                
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { type ->
                                        when (type) {
                                            SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            SongSortType.NAME -> R.string.sort_by_name
                                            SongSortType.ARTIST -> R.string.sort_by_artist
                                            SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }

                    
                    
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.item.song.id },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            isActive = songWrapper.item.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                        tint = rememberAdaptiveOnColor(),
                                    )
                                }
                            },
                            isSelected = songWrapper.isSelected && selection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist,
                                                        items = songs!!.map { it.toSafeMediaItem() },
                                                        startIndex = songs!!.indexOfFirst { it.id == songWrapper.item.id },
                                                    )
                                                )
                                            }
                                        } else {
                                            songWrapper.isSelected = !songWrapper.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) selection = true
                                        wrappedSongs.forEach { it.isSelected = false }
                                        songWrapper.isSelected = true
                                    },
                                )
                                .animateItem(),
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }


        
        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleLarge,
                            color = rememberAdaptiveOnColor(),
                        )
                    }

                    isSearching -> {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = rememberAdaptiveOnColor().copy(alpha = 0.4f),
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = rememberAdaptiveOnColor()),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    else -> {
                        if (!transparentAppBar) {
                            Text(
                                text = playlist,
                                style = MaterialTheme.typography.titleLarge,
                                color = rememberAdaptiveOnColor(),
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        when {
                            isSearching -> {
                                isSearching = false
                                searchQuery = TextFieldValue()
                                focusManager.clearFocus()
                            }

                            selection -> {
                                selection = false
                            }

                            else -> {
                                navController.navigateUp()
                            }
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null,
                        tint = rememberAdaptiveOnColor(),
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null,
                            tint = rememberAdaptiveOnColor(),
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null, tint = rememberAdaptiveOnColor()
                        )
                    }
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = rememberAdaptiveOnColor(),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            scrollBehavior = scrollBehavior,
            modifier = Modifier.onGloballyPositioned { coords ->
                topBarHeight = with(density) { coords.size.height.toDp() }
            }
        )

    }
}


   
                                                                   
                                                                                                      
                                                                           
   
private fun Song.toSafeMediaItem(): MediaItem {
    return if (song.id.startsWith("device_")) {
        val numericId = song.id.removePrefix("device_").toLongOrNull() ?: 0L
        val uri = Uri.withAppendedPath(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            numericId.toString()
        )
        val extras = android.os.Bundle().apply {
            putString("mediaId", song.id)
            putBoolean("isLocal", true)
        }
        MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(uri)
            .setCustomCacheKey(song.id)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(extras)
                    .build()
            )
            .setTag(toMediaMetadata())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(artists.joinToString { it.name })
                    .setAlbumTitle(song.albumName)
                    .setArtworkUri(song.thumbnailUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    } else {
        toMediaItem()
    }
}