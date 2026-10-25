/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.ArtistItem
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.SuggestionItemHeight
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.StreamingListItem
import com.betsy.music.ui.menu.StreamingAlbumMenu
import com.betsy.music.ui.menu.StreamingArtistMenu
import com.betsy.music.ui.menu.StreamingPlaylistMenu
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.flow.drop

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    ownerId: String = "",
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query, ownerId) {
        viewModel.query.value = query
        viewModel.ownerId.value = ownerId
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            WindowInsets.systemBars
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
    ) {
        item {
            Text(
                text = (stringResource(R.string.SearchHistory)),
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(
            items = viewState.history,
            key = { it.query },
        ) { history ->
            SuggestionItem(
                query = history.query,
                online = false,
                onClick = {
                    onSearch(history.query)
                    onDismiss()
                },
                onDelete = {
                    database.query {
                        delete(history)
                    }
                },
                onFillTextField = {
                    onQueryChange(
                        TextFieldValue(
                            text = history.query,
                            selection = TextRange(history.query.length),
                        ),
                    )
                },
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
            )
        }
        if (viewState.suggestions.isNotEmpty()) {
            item {
                Text(
                    text = (stringResource(R.string.Sujestions)),
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        items(
            items = viewState.suggestions,
            key = { it },
        ) { query ->
            SuggestionItem(
                query = query,
                online = true,
                onClick = {
                    onSearch(query)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(
                        TextFieldValue(
                            text = query,
                            selection = TextRange(query.length),
                        ),
                    )
                },
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
            )
        }

        if (viewState.items.isNotEmpty()) {
            item {
                Text(
                    text = (stringResource(R.string.SearchResutls)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(
            items = viewState.items,
            key = { it.id },
        ) { item ->
            StreamingListItem(
                item = item,
                isActive =
                    when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        else -> false
                    },
                isPlaying = isPlaying,
                modifier =
                    Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                StreamingQueue(
                                                    WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata()
                                                ),
                                            )
                                            onDismiss()
                                        }
                                    }

                                    is AlbumItem -> {
                                        navController.navigate("album/${item.id}")
                                        onDismiss()
                                    }

                                    is ArtistItem -> {
                                        navController.navigate("artist/${item.id}")
                                        onDismiss()
                                    }

                                    is PlaylistItem -> {
                                        navController.navigate("online_playlist/${item.id}")
                                        onDismiss()
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        .animateItem()
            )
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    showFillButton: Boolean = true,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = adaptiveColor.copy(alpha = 0.08f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(SuggestionItemHeight)
                .padding(horizontal = 12.dp)
        ) {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
                    .alpha(0.7f),
                tint = adaptiveColor
            )

            Text(
                text = query,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = adaptiveColor
            )

            if (!online) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.alpha(0.7f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.delete),
                        tint = adaptiveColor
                    )
                }
            }

            if (showFillButton) {
                IconButton(
                    onClick = onFillTextField,
                    modifier = Modifier.alpha(0.7f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_top_left),
                        contentDescription = stringResource(R.string.fill_search),
                        tint = adaptiveColor
                    )
                }
            }
        }
    }
}