/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.AlbumFilter
import com.betsy.music.constants.AlbumFilterKey
import com.betsy.music.constants.AlbumSortDescendingKey
import com.betsy.music.constants.AlbumSortType
import com.betsy.music.constants.AlbumSortTypeKey
import com.betsy.music.constants.AlbumViewTypeKey
import com.betsy.music.constants.CONTENT_TYPE_ALBUM
import com.betsy.music.constants.CONTENT_TYPE_HEADER
import com.betsy.music.constants.GridItemSize
import com.betsy.music.constants.GridItemsSizeKey
import com.betsy.music.constants.GridThumbnailHeight
import com.betsy.music.constants.LibraryViewType
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.ui.component.EmptyPlaceholder
import com.betsy.music.ui.component.LibraryAlbumGridItem
import com.betsy.music.ui.component.LibraryAlbumListItem
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.SortHeader
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.LibraryAlbumsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
) {
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
    var filterHeight by remember { mutableStateOf(0.dp) }

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        AlbumSortTypeKey,
        AlbumSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)

    
    val filterContent = @Composable {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            
            Surface(
                onClick = onDeselect,
                shape = RoundedCornerShape(50),
                color = accentColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
                modifier = Modifier.height(34.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.albums),
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            
            listOf(
                AlbumFilter.LIKED to stringResource(R.string.filter_liked),
                AlbumFilter.LIBRARY to stringResource(R.string.filter_library),
                AlbumFilter.PLAYLIST to stringResource(R.string.playlists),
            ).forEach { (value, label) ->
                val isSelected = filter == value
                Surface(
                    onClick = { filter = value },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) accentColor.copy(alpha = 0.18f) else adaptiveColor.copy(
                        alpha = 0.06f
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) accentColor.copy(alpha = 0.7f) else adaptiveColor.copy(alpha = 0.15f),
                    ),
                    modifier = Modifier.height(34.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) accentColor else adaptiveColor.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }

    val allAlbumsRaw by viewModel.allAlbums.collectAsState()
    val albums = remember(allAlbumsRaw, isStreamingMode) {
        if (isStreamingMode) allAlbumsRaw.filter { !it.id.startsWith("local_") }
        else allAlbumsRaw.filter { it.id.startsWith("local_") }
    }
    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        AlbumSortType.CREATE_DATE -> R.string.sort_by_create_date
                        AlbumSortType.NAME -> R.string.sort_by_name
                        AlbumSortType.ARTIST -> R.string.sort_by_artist
                        AlbumSortType.YEAR -> R.string.sort_by_year
                        AlbumSortType.SONG_COUNT -> R.string.sort_by_song_count
                        AlbumSortType.LENGTH -> R.string.sort_by_length
                        AlbumSortType.PLAY_TIME -> R.string.sort_by_play_time
                    }
                },
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.n_album, albums.size, albums.size),
                style = MaterialTheme.typography.titleSmall,
                color = adaptiveColor.copy(alpha = 0.55f),
            )
            IconButton(
                onClick = { viewType = viewType.toggle() },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            ) {
                Icon(
                    painter = painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        }
                    ),
                    contentDescription = null,
                    tint = adaptiveColor,
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                    ),
                    modifier = Modifier.padding(top = topBarHeight + filterHeight),
                ) {
                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                    albums.let { albums ->
                        if (albums.isEmpty()) {
                            item {
                                EmptyPlaceholder(
                                    icon = R.drawable.album,
                                    text = stringResource(R.string.library_album_empty),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        items(
                            items = albums,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ALBUM }) { album ->
                            LibraryAlbumListItem(
                                navController = navController,
                                menuState = menuState,
                                album = album,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp
                    ),
                    contentPadding = PaddingValues(
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                    ),
                    modifier = Modifier.padding(top = topBarHeight + filterHeight),
                ) {
                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) { headerContent() }
                    albums.let { albums ->
                        if (albums.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyPlaceholder(
                                    icon = R.drawable.album,
                                    text = stringResource(R.string.library_album_empty),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        items(
                            items = albums,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ALBUM }) { album ->
                            LibraryAlbumGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                album = album,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
        }

        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topBarHeight)
                .onGloballyPositioned { coords ->
                    filterHeight = with(density) { coords.size.height.toDp() }
                }
        ) {
            filterContent()
        }

        TopAppBar(
            title = { Text(stringResource(R.string.albums), color = rememberAdaptiveOnColor()) },
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