/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.library

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.CONTENT_TYPE_HEADER
import com.betsy.music.constants.CONTENT_TYPE_SONG
import com.betsy.music.constants.SongFilter
import com.betsy.music.constants.SongFilterKey
import com.betsy.music.constants.SongSortDescendingKey
import com.betsy.music.constants.SongSortType
import com.betsy.music.constants.SongSortTypeKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.SongListItem
import com.betsy.music.ui.component.SortHeader
import com.betsy.music.ui.menu.SelectionSongMenu
import com.betsy.music.ui.menu.SongMenu
import com.betsy.music.ui.utils.ItemWrapper
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.LibrarySongsViewModel
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(56.dp) }
    var pinnedHeight by remember { mutableStateOf(0.dp) }

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)

    val allSongsRaw by viewModel.allSongs.collectAsState()
    val songs = remember(allSongsRaw, isStreamingMode) {
        if (isStreamingMode) allSongsRaw.filter { !it.song.id.startsWith("device_") }
        else allSongsRaw.filter { it.song.id.startsWith("device_") }
    }

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    val wrappedSongs = songs.map { item -> ItemWrapper(item) }.toMutableList()
    var selection by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    
    val controlsContent = @Composable {
        if (songs.isNotEmpty()) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Button(
                    onClick  = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.queue_all_songs),
                                items = songs.map { it.toSafeMediaItem() },
                            )
                        )
                    },
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                ) {
                    Icon(
                        painterResource(R.drawable.play), null,
                        tint     = Color(0xFF071520),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.weight(1f).then(Modifier.size(8.dp)))
                    Text(
                        stringResource(R.string.play),
                        color      = Color(0xFF071520),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                    )
                }
                androidx.compose.material3.Button(
                    onClick  = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.queue_all_songs),
                                items = songs.shuffled().map { it.toSafeMediaItem() },
                            )
                        )
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.12f)
                    ),
                    border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                ) {
                    Icon(
                        painterResource(R.drawable.shuffle), null,
                        tint     = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(R.string.shuffle),
                        color      = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                    )
                }
            }
        }
    }

    
    val filterContent = @Composable {
        Row(
            modifier              = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            
            Surface(
                onClick  = onDeselect,
                shape    = RoundedCornerShape(50),
                color    = accentColor.copy(alpha = 0.18f),
                border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
                modifier = Modifier.height(34.dp),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint               = accentColor,
                        modifier           = Modifier.size(14.dp),
                    )
                    Text(
                        text       = stringResource(R.string.songs),
                        color      = accentColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            
            listOf(
                SongFilter.LIKED      to stringResource(R.string.filter_liked),
                SongFilter.LIBRARY    to stringResource(R.string.filter_library),
                SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded),
            ).forEach { (value, label) ->
                val isSelected = filter == value
                Surface(
                    onClick  = { filter = value },
                    shape    = RoundedCornerShape(50),
                    color    = if (isSelected) accentColor.copy(alpha = 0.18f) else adaptiveColor.copy(alpha = 0.06f),
                    border   = BorderStroke(
                        1.dp,
                        if (isSelected) accentColor.copy(alpha = 0.7f) else adaptiveColor.copy(alpha = 0.15f),
                    ),
                    modifier = Modifier.height(34.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier.padding(horizontal = 14.dp),
                    ) {
                        Text(
                            text       = label,
                            color      = if (isSelected) accentColor else adaptiveColor.copy(alpha = 0.55f),
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state          = lazyListState,
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            ),
            modifier       = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight + pinnedHeight),
        ) {
            
            item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        IconButton(onClick = { selection = false }) {
                            Icon(
                                painter            = painterResource(R.drawable.close),
                                contentDescription = null,
                                tint               = adaptiveColor,
                            )
                        }
                        Text(
                            text     = pluralStringResource(R.plurals.n_song, count, count),
                            modifier = Modifier.weight(1f),
                            color    = adaptiveColor,
                        )
                        IconButton(
                            onClick = {
                                if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false }
                                else wrappedSongs.forEach { it.isSelected = true }
                            },
                        ) {
                            Icon(
                                painter            = painterResource(
                                    if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                                ),
                                contentDescription = null,
                                tint               = adaptiveColor,
                            )
                        }
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                                        onDismiss     = menuState::dismiss,
                                        clearAction   = { selection = false },
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter            = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint               = adaptiveColor,
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(horizontal = 16.dp),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides adaptiveColor) {
                                SortHeader(
                                    sortType               = sortType,
                                    sortDescending         = sortDescending,
                                    onSortTypeChange       = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText           = { sortType ->
                                        when (sortType) {
                                            SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            SongSortType.NAME        -> R.string.sort_by_name
                                            SongSortType.ARTIST      -> R.string.sort_by_artist
                                            SongSortType.PLAY_TIME   -> R.string.sort_by_play_time
                                        }
                                    },
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text  = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = adaptiveColor.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }

            
            itemsIndexed(
                items       = wrappedSongs,
                key         = { _, item -> item.item.song.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG },
            ) { index, songWrapper ->
                SongListItem(
                    song            = songWrapper.item,
                    showInLibraryIcon = true,
                    isActive        = songWrapper.item.id == mediaMetadata?.id,
                    isPlaying       = isPlaying,
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
                                tint               = adaptiveColor.copy(alpha = 0.55f),
                            )
                        }
                    },
                    isSelected = songWrapper.isSelected && selection,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (!selection) {
                                    if (songWrapper.item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title      = context.getString(R.string.queue_all_songs),
                                                items      = songs.map { it.toSafeMediaItem() },
                                                startIndex = index,
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

        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topBarHeight)
                .onGloballyPositioned { coords ->
                    pinnedHeight = with(density) { coords.size.height.toDp() }
                }
        ) {
            filterContent()
            controlsContent()
        }

        TopAppBar(
            title = { Text(stringResource(R.string.songs), color = rememberAdaptiveOnColor()) },
            navigationIcon = {
                IconButton(onClick = onDeselect) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        tint = rememberAdaptiveOnColor(),
                    )
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


   
                                                                   
   
private fun com.betsy.music.db.entities.Song.toSafeMediaItem(): MediaItem {
    return if (song.id.startsWith("device_")) {
        val numericId = song.id.removePrefix("device_").toLongOrNull() ?: 0L
        val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, numericId.toString())
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