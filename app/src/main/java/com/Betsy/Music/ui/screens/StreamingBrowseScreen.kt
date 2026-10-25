/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.ArtistItem
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.ListItemHeight
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.IconButton
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.NavigationTitle
import com.betsy.music.ui.component.StreamingGridItem
import com.betsy.music.ui.component.StreamingListItem
import com.betsy.music.ui.component.shimmer.ListItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.ui.menu.StreamingAlbumMenu
import com.betsy.music.ui.menu.StreamingArtistMenu
import com.betsy.music.ui.menu.StreamingPlaylistMenu
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.ui.utils.SnapLayoutInfoProvider
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.viewmodels.StreamingBrowseViewModel
import com.betsy.music.ui.theme.rememberAdaptiveOnColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StreamingBrowseScreen(
    navController: NavController,
    viewModel: StreamingBrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val browseResult by viewModel.result.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val songsLazyGridState = rememberLazyGridState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        val snapLayoutInfoProviderSongs =
            remember(songsLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = songsLazyGridState,
                )
            }
        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (browseResult == null) {
                item {
                    ShimmerHost {
                        repeat(8) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            browseResult?.items?.fastForEach {
                if (it.items.isNotEmpty()) {
                    it.title?.let { title ->
                        item {
                            NavigationTitle(title)
                        }
                    }

                    if ((it.items.firstOrNull() as? SongItem)?.album != null) {
                        item {
                            LazyHorizontalGrid(
                                state = songsLazyGridState,
                                rows = GridCells.Fixed(5),
                                flingBehavior =
                                    rememberSnapFlingBehavior(
                                        snapLayoutInfoProviderSongs,
                                    ),
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight * 5),
                            ) {
                                items(
                                    items = it.items,
                                ) { song ->
                                    Box(Modifier.width(350.dp)) {
                                        StreamingListItem(
                                            item = song as SongItem,
                                            isActive = mediaMetadata?.id == song.id,
                                            isPlaying = isPlaying,
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            StreamingSongMenu(
                                                                song = song,
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
                                            modifier =
                                                Modifier
                                                    .clickable {
                                                        if (song.id == mediaMetadata?.id) {
                                                            playerConnection.player.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                StreamingQueue(
                                                                    WatchEndpoint(
                                                                        videoId = song.id,
                                                                    ),
                                                                    song.toMediaMetadata(),
                                                                ),
                                                            )
                                                        }
                                                    }
                                                    .animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            LazyRow {
                                items(
                                    items = it.items,
                                ) { item ->
                                    StreamingGridItem(
                                        item = item,
                                        isActive =
                                            when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                            Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    StreamingQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata(),
                                                                    ),
                                                                )

                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                            is ArtistItem -> navController.navigate(
                                                                "artist/${item.id}"
                                                            )

                                                            is PlaylistItem -> navController.navigate(
                                                                "online_playlist/${item.id}"
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        menuState.show {
                                                            when (item) {
                                                                is SongItem ->
                                                                    StreamingSongMenu(
                                                                        song = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is AlbumItem ->
                                                                    StreamingAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is ArtistItem ->
                                                                    StreamingArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is PlaylistItem ->
                                                                    StreamingPlaylistMenu(
                                                                        playlist = item,
                                                                        coroutineScope = coroutineScope,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                            }
                                                        }
                                                    },
                                                )
                                                .animateItem(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    TopAppBar(
        title = { Text(browseResult?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}