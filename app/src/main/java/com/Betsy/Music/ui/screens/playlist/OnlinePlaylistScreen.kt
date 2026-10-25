/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.betsy.music.utils.dataStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.AlbumThumbnailSize
import com.betsy.music.constants.HideExplicitKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.constants.ThumbnailCornerRadius
import com.betsy.music.db.entities.AlbumEntity
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.music.db.entities.PlaylistSongMap
import com.betsy.music.extensions.metadata
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.AutoResizeText
import com.betsy.music.ui.component.EmptyPlaceholder
import com.betsy.music.ui.component.FontSizeRange
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.VerticalFastScroller
import com.betsy.music.ui.component.StreamingListItem
import com.betsy.music.ui.component.shimmer.ButtonPlaceholder
import com.betsy.music.ui.component.shimmer.ListItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.ui.component.shimmer.TextPlaceholder
import com.betsy.music.ui.menu.SelectionMediaMetadataMenu
import com.betsy.music.ui.menu.StreamingPlaylistMenu
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.ui.utils.ItemWrapper
import com.betsy.music.utils.AUDIUS_PLAYLIST_ALBUM_PREFIX
import com.betsy.music.utils.makeTimeString
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flowOf
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.OnlinePlaylistViewModel
import com.betsy.music.ui.theme.rememberAdaptiveOnColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()

    
    val audiusAlbumId = playlist?.id?.let { AUDIUS_PLAYLIST_ALBUM_PREFIX + it }
    val libraryAlbum by remember(audiusAlbumId) {
        audiusAlbumId?.let { database.album(it) } ?: flowOf<com.betsy.music.db.entities.Album?>(null)
    }.collectAsState(initial = null)

    
    
    
    val context = LocalContext.current
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }
    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)
    val guestId   = rememberGuestId()
    val ownerId   = remember(prefs, guestId) {
        (prefs[activeAccountIdKey] ?: "").ifBlank { guestId }
    }

    var selection by remember {
        mutableStateOf(false)
    }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    
    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val totalDuration = remember(songs) { songs.sumOf { it.duration ?: 0 } }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.fastAny {
                                    it.name.contains(
                                        query.text,
                                        ignoreCase = true
                                    )
                                }
                    }
            }
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    LaunchedEffect(playlist?.id) {
        if (playlist?.id != null) {
            lazyListState.scrollToItem(0)
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
        .asPaddingValues()
    val wrappedSongs = filteredSongs.map { item -> ItemWrapper(item) }.toMutableList()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = contentPadding.calculateTopPadding(),
            endContentPadding = 0.dp
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start  = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end    = contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = contentPadding.calculateBottomPadding()
                    
                ),
                flingBehavior = ScrollableDefaults.flingBehavior(),
                userScrollEnabled = true,
                modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
            ) {
                playlist.let { playlist ->
                    if (playlist != null) {
                        if (!isSearching) {
                            item {
                                val onColor = rememberAdaptiveOnColor()
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .animateItem(),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(AlbumThumbnailSize)
                                                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                                .fillMaxWidth(),
                                        ) {
                                            AsyncImage(
                                                model = playlist.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                            )
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            AutoResizeText(
                                                text = playlist.title,
                                                color = onColor,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSizeRange = FontSizeRange(16.sp, 22.sp),
                                            )

                                            playlist.author?.let { artist ->
                                                Text(
                                                    buildAnnotatedString {
                                                        withStyle(
                                                            style =
                                                                MaterialTheme.typography.titleMedium
                                                                    .copy(
                                                                        fontWeight = FontWeight.Normal,
                                                                        color = onColor,
                                                                    ).toSpanStyle(),
                                                        ) {
                                                            if (artist.id != null) {
                                                                val link =
                                                                    LinkAnnotation.Clickable(
                                                                        tag = artist.id!!,
                                                                        styles = TextLinkStyles(
                                                                            style = SpanStyle(
                                                                                color = accentColor,
                                                                                textDecoration = TextDecoration.Underline,
                                                                            ),
                                                                        ),
                                                                    ) {
                                                                        navController.navigate("artist/${artist.id!!}")
                                                                    }
                                                                withLink(link) {
                                                                    append(artist.name)
                                                                }
                                                            } else {
                                                                append(artist.name)
                                                            }
                                                        }
                                                    },
                                                )
                                            }

                                            Text(
                                                text =
                                                    pluralStringResource(
                                                        R.plurals.n_song,
                                                        songs.size,
                                                        songs.size,
                                                    ),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = onColor,
                                                fontWeight = FontWeight.Normal,
                                            )

                                            if (totalDuration > 0) {
                                                Text(
                                                    text = makeTimeString(totalDuration * 1000L),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = onColor,
                                                    fontWeight = FontWeight.Normal,
                                                )
                                            }

                                            Row {
                                                if (playlist.id != "LM") {
                                                    IconButton(
                                                        onClick = {
                                                            if (dbPlaylist?.playlist == null) {
                                                                database.transaction {
                                                                    val playlistEntity =
                                                                        PlaylistEntity(
                                                                            name = playlist.title,
                                                                            browseId = playlist.id,
                                                                            isEditable = playlist.isEditable,
                                                                            playEndpointParams = playlist.playEndpoint?.params,
                                                                            shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                                            radioEndpointParams = playlist.radioEndpoint?.params,
                                                                            ownerId     = ownerId,
                                                                            isLocalMode = !isStreamingMode,
                                                                        ).toggleLike()
                                                                    insert(playlistEntity)
                                                                    songs.map(SongItem::toMediaMetadata)
                                                                        .onEach { insert(it) }
                                                                        .mapIndexed { index, song ->
                                                                            PlaylistSongMap(
                                                                                songId = song.id,
                                                                                playlistId = playlistEntity.id,
                                                                                position = index
                                                                            )
                                                                        }
                                                                        .forEach { insert(it) }

                                                                    
                                                                    val now = LocalDateTime.now()
                                                                    val savedAlbum = libraryAlbum?.album
                                                                    if (savedAlbum == null) {
                                                                        insert(
                                                                            AlbumEntity(
                                                                                id = AUDIUS_PLAYLIST_ALBUM_PREFIX + playlist.id,
                                                                                title = playlist.title,
                                                                                thumbnailUrl = playlist.thumbnail,
                                                                                songCount = songs.size,
                                                                                duration = totalDuration,
                                                                                bookmarkedAt = now,
                                                                                inLibrary = now,
                                                                            )
                                                                        )
                                                                    } else {
                                                                        update(savedAlbum.copy(bookmarkedAt = now, inLibrary = now))
                                                                    }
                                                                }
                                                            } else {
                                                                database.transaction {
                                                                    val toggled = dbPlaylist!!.playlist.toggleLike()
                                                                    
                                                                    update(
                                                                        if (toggled.bookmarkedAt != null) toggled.copy(ownerId = ownerId, isLocalMode = !isStreamingMode)
                                                                        else toggled
                                                                    )

                                                                    
                                                                    val liked = toggled.bookmarkedAt != null
                                                                    val now = LocalDateTime.now()
                                                                    val savedAlbum = libraryAlbum?.album
                                                                    if (savedAlbum == null) {
                                                                        if (liked) {
                                                                            insert(
                                                                                AlbumEntity(
                                                                                    id = AUDIUS_PLAYLIST_ALBUM_PREFIX + playlist.id,
                                                                                    title = playlist.title,
                                                                                    thumbnailUrl = playlist.thumbnail,
                                                                                    songCount = songs.size,
                                                                                    duration = totalDuration,
                                                                                    bookmarkedAt = now,
                                                                                    inLibrary = now,
                                                                                )
                                                                            )
                                                                        }
                                                                    } else {
                                                                        update(
                                                                            savedAlbum.copy(
                                                                                bookmarkedAt = if (liked) now else null,
                                                                                inLibrary = if (liked) now else null,
                                                                            )
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                                            ),
                                                            contentDescription = null,
                                                            tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else rememberAdaptiveOnColor()
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        playerConnection.addToQueue(
                                                            items = songs.map { it.toMediaItem() },
                                                        )
                                                    },
                                                    enabled = songs.isNotEmpty(),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.queue_music),
                                                        contentDescription = null,
                                                        tint = onColor,
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            StreamingPlaylistMenu(
                                                                playlist = playlist,
                                                                songs = songs,
                                                                coroutineScope = coroutineScope,
                                                                onDismiss = menuState::dismiss,
                                                                selectAction = { selection = true },
                                                                canSelect = true,
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null,
                                                        tint = onColor,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                playlist.playEndpoint?.let { playerConnection.playQueue(StreamingQueue(it)) }
                                            },
                                            enabled = songs.isNotEmpty() && playlist.playEndpoint != null,
                                            modifier = Modifier.weight(1.4f).height(52.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor,
                                                disabledContainerColor = accentColor.copy(alpha = 0.3f),
                                            ),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.play),
                                                contentDescription = null,
                                                tint = Color(0xFF071520),
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.play),
                                                color = Color(0xFF071520),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                playlist.shuffleEndpoint?.let { playerConnection.playQueue(StreamingQueue(it)) }
                                            },
                                            enabled = songs.isNotEmpty() && playlist.shuffleEndpoint != null,
                                            modifier = Modifier.weight(1f).height(52.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor.copy(alpha = 0.12f),
                                                disabledContainerColor = accentColor.copy(alpha = 0.06f),
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                accentColor.copy(alpha = 0.35f),
                                            ),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                stringResource(R.string.shuffle),
                                                color = accentColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        
                        
                        if (songs.isEmpty()) {
                            item {
                                EmptyPlaceholder(
                                    icon = R.drawable.music_note,
                                    text = stringResource(R.string.playlist_is_empty),
                                )
                            }
                        }

                        items(
                            items = wrappedSongs,
                        ) { song ->
                            StreamingListItem(
                                item = song.item.second,
                                rankNumber = song.item.first + 1,
                                isActive = mediaMetadata?.id == song.item.second.id,
                                isPlaying = isPlaying,
                                isSelected = song.isSelected && selection,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                StreamingSongMenu(
                                                    song = song.item.second,
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
                                        .combinedClickable(
                                            enabled = !hideExplicit || !song.item.second.explicit,
                                            onClick = {
                                                if (!selection) {
                                                    if (song.item.second.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.service.getAutomix(
                                                            playlistId = playlist.id
                                                        )
                                                        playerConnection.playQueue(
                                                            StreamingQueue(
                                                                song.item.second.endpoint
                                                                    ?: WatchEndpoint(
                                                                        videoId =
                                                                            song.item.second
                                                                                .id,
                                                                    ),
                                                                song.item.second.toMediaMetadata(),
                                                            ),
                                                        )
                                                    }
                                                } else {
                                                    song.isSelected = !song.isSelected
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (!selection) {
                                                    selection = true
                                                }
                                                wrappedSongs.forEach { it.isSelected = false }
                                                song.isSelected = true
                                            },
                                        )
                                        .alpha(if (hideExplicit && song.item.second.explicit) 0.3f else 1f)
                                        .animateItem(),
                            )
                        }
                    } else {
                        item {
                            ShimmerHost {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .size(AlbumThumbnailSize)
                                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                                    .background(MaterialTheme.colorScheme.onSurface),
                                        )

                                        Spacer(Modifier.width(16.dp))

                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            TextPlaceholder()
                                            TextPlaceholder()
                                            TextPlaceholder()
                                        }
                                    }

                                    Spacer(Modifier.padding(8.dp))

                                    Row {
                                        ButtonPlaceholder(Modifier.weight(1f))

                                        Spacer(Modifier.width(12.dp))

                                        ButtonPlaceholder(Modifier.weight(1f))
                                    }
                                }

                                repeat(6) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge,
                        color = rememberAdaptiveOnColor(),
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
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
                } else {
                    if (showTopBarTitle) {
                        Text(
                            text     = playlist?.title.orEmpty(),
                            style    = MaterialTheme.typography.titleLarge,
                            color    = rememberAdaptiveOnColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else if (selection) {
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
                                SelectionMediaMetadataMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.second.toMediaItem().metadata!! },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                    currentItems = emptyList()
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null, tint = rememberAdaptiveOnColor()
                        )
                    }
                } else {
                    if (!isSearching) {
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
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            windowInsets   = WindowInsets(0, 0, 0, 0)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                    .align(Alignment.BottomCenter),
        )
    }
}