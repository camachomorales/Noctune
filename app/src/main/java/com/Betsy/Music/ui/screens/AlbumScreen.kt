/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import com.betsy.music.ui.theme.rememberAdaptivePanelColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalDownloadUtil
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.db.entities.Album
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.playback.ExoDownloadService
import com.betsy.music.playback.queues.LocalAlbumRadio
import com.betsy.music.ui.component.IconButton
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.NavigationTitle
import com.betsy.music.ui.component.SongListItem
import com.betsy.music.ui.component.StreamingGridItem
import com.betsy.music.ui.component.shimmer.ListItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.ui.component.shimmer.TextPlaceholder
import com.betsy.music.ui.menu.AlbumMenu
import com.betsy.music.ui.menu.SelectionSongMenu
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.utils.rememberPreference
import com.betsy.music.ui.menu.SongMenu
import com.betsy.music.ui.menu.StreamingAlbumMenu
import com.betsy.music.ui.utils.ItemWrapper
import com.betsy.music.viewmodels.AlbumViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()

    val wrappedSongs = albumWithSongs?.songs?.map { item -> ItemWrapper(item) }?.toMutableList()
    var selection by remember {
        mutableStateOf(false)
    }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val lazyListState = rememberLazyListState()
    var topBarHeight by remember { mutableStateOf(56.dp) }
    val density = LocalDensity.current

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight)
        ) {
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                item(key = "album_header") {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(8.dp))

                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text       = albumWithSongs.album.title,
                                color      = rememberAdaptiveOnColor(),
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    append(albumWithSongs.artists.joinToString { it.name })
                                    if (albumWithSongs.album.year != null) append(" • ${albumWithSongs.album.year}")
                                    append(" • ${albumWithSongs.songs.size} tracks")
                                    val totalDuration = albumWithSongs.songs.sumOf { it.song.duration ?: 0 }
                                    val hours   = totalDuration / 3600
                                    val minutes = (totalDuration % 3600) / 60
                                    if (hours > 0) append(" • ${hours}h ${minutes}m") else append(" • ${minutes}m")
                                },
                                color      = accentColor,
                                fontSize   = 14.sp,
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
                                    .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model              = albumWithSongs.album.thumbnailUrl,
                                    contentDescription = null,
                                    modifier           = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale       = ContentScale.Crop,
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                        playerConnection.player.pause()
                                    } else if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                                        playerConnection.player.play()
                                    } else {
                                        playerConnection.service.getAutomix(playlistId)
                                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                                    }
                                },
                                modifier = Modifier.weight(1.4f).height(52.dp),
                                shape    = RoundedCornerShape(16.dp),
                                colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = accentColor
                                ),
                            ) {
                                Icon(
                                    painterResource(
                                        if (isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id)
                                            R.drawable.pause else R.drawable.play
                                    ),
                                    null,
                                    tint     = Color(0xFF071520),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.play),
                                    color      = Color(0xFF071520),
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 15.sp,
                                )
                            }
                            androidx.compose.material3.Button(
                                onClick = {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled()))
                                    )
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape    = RoundedCornerShape(16.dp),
                                colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.12f)
                                ),
                                border   = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            ) {
                                Icon(painterResource(R.drawable.shuffle), null, tint = accentColor, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.shuffle), color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            
                            androidx.compose.material3.Button(
                                onClick = {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                            albumWithSongs.songs.forEach { song ->
                                                DownloadService.sendRemoveDownload(
                                                    context, ExoDownloadService::class.java, song.id, false
                                                )
                                            }
                                        }
                                        else -> {
                                            albumWithSongs.songs.forEach { song ->
                                                val downloadRequest = DownloadRequest
                                                    .Builder(song.id, song.id.toUri())
                                                    .setCustomCacheKey(song.id)
                                                    .setData(song.song.title.toByteArray())
                                                    .build()
                                                DownloadService.sendAddDownload(
                                                    context, ExoDownloadService::class.java, downloadRequest, false
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.12f)
                                ),
                                border   = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            ) {
                                when (downloadState) {
                                    Download.STATE_COMPLETED -> Icon(painterResource(R.drawable.offline), null, tint = accentColor, modifier = Modifier.size(16.dp))
                                    Download.STATE_DOWNLOADING -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = accentColor)
                                    else -> Icon(painterResource(R.drawable.download), null, tint = accentColor, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = when (downloadState) {
                                        Download.STATE_COMPLETED  -> stringResource(R.string.download)
                                        Download.STATE_DOWNLOADING -> stringResource(R.string.downloading)
                                        else -> stringResource(R.string.save)
                                    },
                                    color    = accentColor,
                                    fontSize = 13.sp,
                                )
                            }
                            
                            androidx.compose.material3.Button(
                                onClick = {
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = Album(albumWithSongs.album, albumWithSongs.artists),
                                            navController = navController,
                                            onDismiss     = menuState::dismiss,
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = accentColor.copy(alpha = 0.12f)
                                ),
                                border   = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            ) {
                                Icon(painterResource(R.drawable.more_vert), null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.more), color = accentColor, fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(
                            color    = rememberAdaptivePanelColor(0.08f),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                
                
                
                
                if (wrappedSongs != null && wrappedSongs.isNotEmpty()) {
                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, song -> song.item.id },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong  = songWrapper.item,
                                                navController = navController,
                                                onDismiss     = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter            = painterResource(R.drawable.more_vert),
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
                                            if (songWrapper.item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(
                                                        albumWithSongs,
                                                        startIndex = wrappedSongs.indexOf(songWrapper)
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
                }

                
                if (otherVersions.isNotEmpty()) {
                    item(key = "other_versions_title") {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                            modifier = Modifier.animateItem()
                        )
                    }
                    item(key = "other_versions_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        ) {
                            items(
                                items = otherVersions.distinctBy { it.id },
                                key = { it.id },
                            ) { item ->
                                StreamingGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    StreamingAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }
            } else {
                
                item(key = "loading_indicator") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            }
        }

        
        TopAppBar(
            title = {
                if (selection) {
                    val count = wrappedSongs?.count { it.isSelected } ?: 0
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge,
                        color = rememberAdaptiveOnColor(),
                    )
                } else {
                    if (!transparentAppBar) {
                        Text(
                            text = albumWithSongs?.album?.title.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = rememberAdaptiveOnColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
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
                    val count = wrappedSongs?.count { it.isSelected } ?: 0
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs?.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs?.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs?.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs?.filter { it.isSelected }!!
                                        .map { it.item },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false }
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null, tint = rememberAdaptiveOnColor()
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            modifier = Modifier.onGloballyPositioned { coords ->
                topBarHeight = with(density) { coords.size.height.toDp() }
            }
        )
    }
}

suspend fun saveAlbumImageToGallery(context: Context, imageUrl: String, albumTitle: String) {
    try {
        
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        val drawable = context.imageLoader.execute(request).drawable

        if (drawable != null) {
            val bitmap = drawable.toBitmap()

            
            val displayName = "${albumTitle.replace(" ", "_")}.png"
            val mimeType = "image/png" 

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Save Picture Success",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "X",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}