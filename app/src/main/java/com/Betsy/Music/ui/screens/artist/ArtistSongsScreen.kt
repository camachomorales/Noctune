/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.ArtistSongSortDescendingKey
import com.betsy.music.constants.ArtistSongSortType
import com.betsy.music.constants.ArtistSongSortTypeKey
import com.betsy.music.constants.CONTENT_TYPE_HEADER
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.ui.component.HideOnScrollFAB
import com.betsy.music.ui.component.IconButton
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.SongListItem
import com.betsy.music.ui.component.SortHeader
import com.betsy.music.ui.component.VerticalFastScroller
import com.betsy.music.ui.menu.SelectionSongMenu
import com.betsy.music.ui.menu.SongMenu
import com.betsy.music.ui.utils.ItemWrapper
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.ArtistSongsViewModel
import com.betsy.music.ui.theme.rememberAdaptiveOnColor

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistSongsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistSongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        ArtistSongSortTypeKey,
        ArtistSongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        ArtistSongSortDescendingKey,
        true
    )
    val artist by viewModel.artist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val lazyListState = rememberLazyListState()

    
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isSearching by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    
    var selection by remember { mutableStateOf(false) }

    
    val wrappedSongs = remember(songs) {
        songs.map { song -> ItemWrapper(song) }
    }

    
    val searchQueryStr = searchQuery.text.trim()
    val filteredSongs = if (searchQueryStr.isEmpty()) {
        wrappedSongs
    } else {
        wrappedSongs.filter { wrapper ->
            wrapper.item.song.title.contains(searchQueryStr, ignoreCase = true) ||
                    wrapper.item.artists.joinToString("")
                        .contains(searchQueryStr, ignoreCase = true)
        }
    }

    
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        VerticalFastScroller(
            listState = lazyListState,
            thumbColor = MaterialTheme.colorScheme.primary,
            topContentPadding = 0.dp,
            endContentPadding = 8.dp
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                if (!isSearching) {
                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        ArtistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        ArtistSongSortType.NAME -> R.string.sort_by_name
                                        ArtistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                },
                            )

                            Spacer(Modifier.weight(1f))

                            Text(
                                text = pluralStringResource(
                                    R.plurals.n_song,
                                    filteredSongs.size,
                                    filteredSongs.size
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = rememberAdaptiveOnColor().copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = filteredSongs,
                    key = { _, item -> item.item.id },
                ) { index, songWrapper ->
                    SongListItem(
                        song = songWrapper.item,
                        showInLibraryIcon = true,
                        isActive = songWrapper.item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSelected = songWrapper.isSelected && selection,
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (songWrapper.item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = context.getString(R.string.queue_all_songs),
                                                        items = filteredSongs.map { it.item.toMediaItem() },
                                                        startIndex = index,
                                                    ),
                                                )
                                            }
                                        } else {
                                            songWrapper.isSelected = !songWrapper.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                        }
                                        wrappedSongs.forEach { it.isSelected = false }
                                        songWrapper.isSelected = true
                                    },
                                )
                                .animateItem(),
                    )
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
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    isSearching -> {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
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
                        Text(artist?.artist?.name.orEmpty(), color = rememberAdaptiveOnColor())
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
                            }

                            selection -> {
                                selection = false
                                wrappedSongs.forEach { it.isSelected = false }
                            }

                            else -> navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching) {
                            navController.backToMain()
                        }
                    },
                ) {
                    Icon(
                        painterResource(
                            when {
                                isSearching -> R.drawable.close
                                selection -> R.drawable.close
                                else -> R.drawable.arrow_back
                            }
                        ),
                        contentDescription = null,
                        tint = rememberAdaptiveOnColor(),
                    )
                }
            },
            actions = {
                when {
                    selection -> {
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
                                        clearAction = {
                                            selection = false
                                            wrappedSongs.forEach { it.isSelected = false }
                                        },
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

                    !isSearching -> {
                        
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
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        )

        
        if (!isSearching && !selection && filteredSongs.isNotEmpty()) {
            HideOnScrollFAB(
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                containerColor = accentColor,
                contentColor = Color(0xFF071520),
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = artist?.artist?.name,
                            items = filteredSongs.shuffled().map { it.item.toMediaItem() },
                        ),
                    )
                },
            )
        }
    }
}