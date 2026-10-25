/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.betsy.audius.compat.AudiusApi.SearchFilter.Companion.FILTER_ALBUM
import com.betsy.audius.compat.AudiusApi.SearchFilter.Companion.FILTER_ARTIST
import com.betsy.audius.compat.AudiusApi.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.betsy.audius.compat.AudiusApi.SearchFilter.Companion.FILTER_SONG
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
import com.betsy.music.constants.AppBarHeight
import com.betsy.music.constants.SearchFilterHeight
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.ChipsRow
import com.betsy.music.ui.component.EmptyPlaceholder
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.NavigationTitle
import com.betsy.music.ui.component.StreamingListItem
import com.betsy.music.ui.component.shimmer.ListItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.ui.menu.StreamingAlbumMenu
import com.betsy.music.ui.menu.StreamingArtistMenu
import com.betsy.music.ui.menu.StreamingPlaylistMenu
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.utils.iaIdentifierOf
import com.betsy.music.utils.isIaAlbumId
import com.betsy.music.utils.localizedStreamingTitle
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch
import com.betsy.music.ui.theme.rememberAdaptiveOnColor

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val menuState        = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic           = LocalHapticFeedback.current
    val isPlaying     by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState  = rememberLazyListState()

    val searchFilter  by viewModel.filter.collectAsState()
    val searchSummary  = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let { viewModel.viewStateMap[it] }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(Int, YTItem) -> Unit = { index: Int, item: YTItem ->
        
        val isIaAlbum = item is AlbumItem && isIaAlbumId(item.id)
        val longClick: () -> Unit = {
            if (!isIaAlbum) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                menuState.show {
                    when (item) {
                        is SongItem ->
                            StreamingSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                        is AlbumItem ->
                            StreamingAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                        is ArtistItem ->
                            StreamingArtistMenu(artist = item, onDismiss = menuState::dismiss)
                        is PlaylistItem ->
                            StreamingPlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                    }
                }
            }
        }
        StreamingListItem(
            item = item,
            
            rankNumber = index + 1,
            isActive = when (item) {
                is SongItem  -> mediaMetadata?.id == item.id
                is AlbumItem -> mediaMetadata?.album?.id == item.id
                else         -> false
            },
            isPlaying = isPlaying,
            trailingContent = {
                if (!isIaAlbum) {
                    IconButton(onClick = longClick) {
                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null, tint = rememberAdaptiveOnColor())
                    }
                }
            },
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> {
                                if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                else playerConnection.playQueue(
                                    StreamingQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata())
                                )
                            }
                            is AlbumItem    ->
                                if (isIaAlbum) navController.navigate("ia_item/${Uri.encode(iaIdentifierOf(item.id))}")
                                else navController.navigate("album/${item.id}")
                            is ArtistItem   -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = longClick,
                )
                .animateItem(),
        )
    }

    
    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = SearchFilterHeight))
            .add(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .asPaddingValues(),
    ) {
        if (searchFilter == null) {
            searchSummary?.summaries?.forEach { summary ->
                item {
                    NavigationTitle(localizedStreamingTitle(summary.title))
                }
                itemsIndexed(
                    items       = summary.items,
                    key         = { _, item -> "${summary.title}/${item.id}" },
                    itemContent = ytItemContent,
                )
            }

            if (searchSummary?.summaries?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        } else {
            itemsIndexed(
                items       = itemsPage?.items.orEmpty().distinctBy { it.id },
                key         = { _, item -> item.id },
                itemContent = ytItemContent,
            )

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost { repeat(3) { ListItemPlaceHolder() } }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }

        if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
            item {
                ShimmerHost { repeat(8) { ListItemPlaceHolder() } }
            }
        }
    }

    
    ChipsRow(
        chips = listOf(
            null                      to stringResource(R.string.filter_all),
            FILTER_SONG               to stringResource(R.string.filter_songs),
            FILTER_ALBUM              to stringResource(R.string.filter_albums),
            FILTER_ARTIST             to stringResource(R.string.filter_artists),
            FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
        ),
        currentValue  = searchFilter,
        onValueUpdate = {
            if (viewModel.filter.value != it) viewModel.filter.value = it
            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
        },
        selectedColor = accentColor,
        modifier = Modifier
            .background(Color.Transparent)   
            .windowInsetsPadding(
                WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add(WindowInsets(top = AppBarHeight))
            )
            .fillMaxWidth(),
    )
}








