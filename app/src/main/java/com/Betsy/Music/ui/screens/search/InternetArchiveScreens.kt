/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.ui.screens.search

import java.time.LocalDateTime
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.internetarchive.IaItem
import com.betsy.internetarchive.IaItemDetails
import com.betsy.internetarchive.IaTrack
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.AlbumThumbnailSize
import com.betsy.music.constants.ListThumbnailSize
import com.betsy.music.constants.ThumbnailCornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.AlbumEntity
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.music.db.entities.PlaylistSongMap
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.iaPlaylistIdOf
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.ui.component.AutoResizeText
import com.betsy.music.ui.component.EmptyPlaceholder
import com.betsy.music.ui.component.FontSizeRange
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.ListItem
import com.betsy.music.ui.component.NavigationTitle
import com.betsy.music.ui.component.PlayingIndicatorBox
import com.betsy.music.ui.component.ResultThumbnail
import com.betsy.music.ui.component.shimmer.ListItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.ui.menu.IaTrackMenu
import com.betsy.music.utils.IA_ALBUM_PREFIX
import com.betsy.music.utils.makeTimeString
import com.betsy.music.utils.rememberPreference
import com.betsy.music.utils.toMediaMetadata
import java.text.NumberFormat





private sealed interface IaLoad<out T> {
    object Loading : IaLoad<Nothing>
    data class Ok<T>(val value: T) : IaLoad<T>
    data class Err(val error: Throwable) : IaLoad<Nothing>
}

@Composable
private fun <T> rememberIaLoad(key: Any?, block: suspend () -> Result<T>): IaLoad<T> {
    var state by remember(key) { mutableStateOf<IaLoad<T>>(IaLoad.Loading) }
    LaunchedEffect(key) {
        state = IaLoad.Loading
        state = block().fold(
            onSuccess = { IaLoad.Ok(it) },
            onFailure = { IaLoad.Err(it) },
        )
    }
    return state
}

@Composable
private fun accentColor(): Color {
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    return remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
}

@Composable
private fun IaThumbnail(url: String, size: Dp, modifier: Modifier = Modifier) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(adaptiveColor.copy(alpha = 0.10f)),
    ) {
        
        Icon(
            painter = painterResource(R.drawable.music_note),
            contentDescription = null,
            tint = adaptiveColor.copy(alpha = 0.6f),
            modifier = Modifier.size(size / 2),
        )
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IaTopBar(title: String, navController: NavController, showTitle: Boolean = true, modifier: Modifier = Modifier) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    TopAppBar(
        title = {
            if (showTitle) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = adaptiveColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = adaptiveColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}





@Composable
fun InternetArchiveSuggestions(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    ownerId: String,
) {
    val database = LocalDatabase.current
    val accent = accentColor()
    val typed = query.trim()
    val history by remember(typed, ownerId) { database.searchHistory(typed, ownerId) }
        .collectAsState(initial = emptyList<com.betsy.music.db.entities.SearchHistory>())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        val shownHistory = history.take(5)
        if (shownHistory.isNotEmpty()) {
            item(key = "ia_history_header") {
                Text(
                    text = stringResource(R.string.SearchHistory),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(items = shownHistory, key = { "ia_history_${it.id}" }) { h ->
                SuggestionItem(
                    query = h.query,
                    online = false,
                    onClick = { onSearch(h.query) },
                    onDelete = { database.query { delete(h) } },
                    onFillTextField = {
                        onQueryChange(TextFieldValue(text = h.query, selection = TextRange(h.query.length)))
                    },
                )
            }
        }
        if (typed.isNotEmpty() && shownHistory.none { it.query == typed }) {
            item(key = "ia_typed_header") {
                Text(
                    text = stringResource(R.string.Sujestions),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item(key = "ia_typed_query") {
                SuggestionItem(
                    query = typed,
                    online = true,
                    onClick = { onSearch(typed) },
                    onFillTextField = {},
                    showFillButton = false,
                )
            }
        }
    }
}





@Composable
fun InternetArchiveSearchResult(navController: NavController, query: String) {
    val accent = accentColor()
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    var items by remember(query) { mutableStateOf(emptyList<IaItem>()) }
    var page by remember(query) { mutableIntStateOf(1) }
    var total by remember(query) { mutableLongStateOf(0L) }
    var loading by remember(query) { mutableStateOf(true) }
    var failed by remember(query) { mutableStateOf(false) }
    var retry by remember(query) { mutableIntStateOf(0) }

    LaunchedEffect(query, page, retry) {
        loading = true
        failed = false
        InternetArchive.client.search(query, page = page)
            .onSuccess { result ->
                items = items + result.items.filter { new -> items.none { it.identifier == new.identifier } }
                total = result.total
            }
            .onFailure { failed = true }
        loading = false
    }

    val lazyListState = rememberLazyListState()
    val hasMore = !failed && items.isNotEmpty() && items.size < total

    
    LaunchedEffect(lazyListState, items.size, loading, hasMore) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "ia_more" }
        }.collect { visible ->
            if (visible && !loading && hasMore) page++
        }
    }

    
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = LocalPlayerAwareWindowInsets.current
            .add(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .asPaddingValues(),
    ) {
        if (items.isNotEmpty()) {
            item(key = "ia_header") {
                NavigationTitle(stringResource(R.string.ia_source_ia))
            }
        }
        itemsIndexed(items = items, key = { _, item -> item.identifier }) { index, item ->
            IaItemRow(
                item = item,
                rankNumber = index + 1,
                onClick = { navController.navigate("ia_item/${Uri.encode(item.identifier)}") },
                modifier = Modifier.animateItem(),
            )
        }
        if (loading && items.isEmpty()) {
            item(key = "ia_loading") {
                ShimmerHost { repeat(8) { ListItemPlaceHolder() } }
            }
        }
        if (hasMore) {
            item(key = "ia_more") {
                ShimmerHost { repeat(3) { ListItemPlaceHolder() } }
            }
        }
        if (failed) {
            item(key = "ia_error") {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.ia_load_error), color = adaptiveColor)
                    TextButton(onClick = { retry++ }) { Text(stringResource(R.string.ia_retry), color = accent) }
                }
            }
        }
        if (!loading && !failed && items.isEmpty()) {
            item(key = "ia_empty") {
                EmptyPlaceholder(
                    icon = R.drawable.search,
                    text = stringResource(R.string.no_results_found),
                )
            }
        }
    }
}


@Composable
private fun IaItemRow(item: IaItem, rankNumber: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val downloadsText = item.downloads.takeIf { it > 0 }?.let {
        stringResource(R.string.ia_downloads_count, NumberFormat.getIntegerInstance().format(it))
    }
    ListItem(
        title = item.title,
        subtitle = listOfNotNull(item.creator, downloadsText).joinToString(" • "),
        thumbnailContent = {
            Box(modifier = Modifier.size(ListThumbnailSize)) {
                ResultThumbnail(url = item.thumbnailUrl, rankNumber = rankNumber)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}





@Composable
fun InternetArchiveItemScreen(navController: NavController, identifier: String) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val uriHandler = LocalUriHandler.current
    val accent = accentColor()
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()

    
    val database = LocalDatabase.current
    val libraryAlbum by database.album(IA_ALBUM_PREFIX + identifier).collectAsState(initial = null)
    val isSaved = libraryAlbum?.album?.bookmarkedAt != null

    
    val libraryPlaylist by database.playlist(iaPlaylistIdOf(identifier)).collectAsState(initial = null)
    val heartContext = LocalContext.current
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }
    val heartPrefs by heartContext.dataStore.data.collectAsState(initial = emptyPreferences())
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)
    val guestId = rememberGuestId()
    val ownerId = remember(heartPrefs, guestId) {
        (heartPrefs[activeAccountIdKey] ?: "").ifBlank { guestId }
    }

    var attempt by remember { mutableIntStateOf(0) }
    val load = rememberIaLoad(identifier to attempt) { InternetArchive.client.details(identifier) }

    val listState = rememberLazyListState()
    val showTopBarTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    fun play(details: IaItemDetails, tracks: List<IaTrack>, index: Int) {
        playerConnection.playQueue(
            ListQueue(
                title = details.item.title,
                items = tracks.map { it.toMediaMetadata().toMediaItem() },
                startIndex = index,
            ),
        )
    }

    val title = (load as? IaLoad.Ok<IaItemDetails>)?.value?.item?.title ?: identifier

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            when (load) {
                is IaLoad.Loading -> item(key = "ia_item_loading") {
                    ShimmerHost { repeat(8) { ListItemPlaceHolder() } }
                }

                is IaLoad.Err -> item(key = "ia_item_error") {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.ia_load_error), color = adaptiveColor)
                        TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.ia_retry), color = accent) }
                    }
                }

                is IaLoad.Ok -> {
                    val details = load.value
                    val tracks = details.tracks
                    val totalDuration = tracks.sumOf { it.durationSeconds ?: 0 }

                    
                    item(key = "ia_item_header") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(AlbumThumbnailSize)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                ) {
                                    ResultThumbnail(url = details.item.thumbnailUrl)
                                }

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    AutoResizeText(
                                        text = details.item.title,
                                        color = adaptiveColor,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSizeRange = FontSizeRange(16.sp, 22.sp),
                                    )

                                    details.item.creator?.let {
                                        Text(
                                            text = it,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Normal,
                                            color = adaptiveColor,
                                        )
                                    }

                                    Text(
                                        text = pluralStringResource(R.plurals.n_song, tracks.size, tracks.size),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = adaptiveColor,
                                    )

                                    if (totalDuration > 0) {
                                        Text(
                                            text = makeTimeString(totalDuration * 1000L),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Normal,
                                            color = adaptiveColor,
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            enabled = tracks.isNotEmpty(),
                                            onClick = {
                                                playerConnection.addToQueue(
                                                    items = tracks.map { it.toMediaMetadata().toMediaItem() },
                                                )
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = null,
                                                tint = adaptiveColor,
                                            )
                                        }

                                        
                                        IconButton(
                                            onClick = {
                                                val liked = !isSaved
                                                val now = LocalDateTime.now()
                                                val playlistId = iaPlaylistIdOf(identifier)
                                                database.transaction {
                                                    
                                                    val existing = libraryAlbum?.album
                                                    if (existing == null) {
                                                        if (liked) {
                                                            insert(
                                                                AlbumEntity(
                                                                    id = IA_ALBUM_PREFIX + identifier,
                                                                    title = details.item.title,
                                                                    thumbnailUrl = details.item.thumbnailUrl,
                                                                    songCount = tracks.size,
                                                                    duration = totalDuration,
                                                                    bookmarkedAt = now,
                                                                    inLibrary = now,
                                                                ),
                                                            )
                                                        }
                                                    } else {
                                                        update(
                                                            existing.copy(
                                                                bookmarkedAt = if (liked) now else null,
                                                                inLibrary = if (liked) now else null,
                                                            ),
                                                        )
                                                    }

                                                    
                                                    val existingPlaylist = libraryPlaylist?.playlist
                                                    if (existingPlaylist == null) {
                                                        if (liked) {
                                                            insert(
                                                                PlaylistEntity(
                                                                    id = playlistId,
                                                                    name = details.item.title,
                                                                    bookmarkedAt = now,
                                                                    ownerId = ownerId,
                                                                    isLocalMode = !isStreamingMode,
                                                                ),
                                                            )
                                                            tracks.map { it.toMediaMetadata() }
                                                                .onEach { insert(it) }
                                                                .forEachIndexed { index, song ->
                                                                    insert(
                                                                        PlaylistSongMap(
                                                                            songId = song.id,
                                                                            playlistId = playlistId,
                                                                            position = index,
                                                                        ),
                                                                    )
                                                                }
                                                        }
                                                    } else {
                                                        update(
                                                            if (liked) {
                                                                existingPlaylist.copy(
                                                                    bookmarkedAt = now,
                                                                    ownerId = ownerId,
                                                                    isLocalMode = !isStreamingMode,
                                                                )
                                                            } else {
                                                                existingPlaylist.copy(bookmarkedAt = null)
                                                            },
                                                        )
                                                    }
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (isSaved) R.drawable.favorite else R.drawable.favorite_border
                                                ),
                                                contentDescription = null,
                                                tint = if (isSaved) MaterialTheme.colorScheme.error else adaptiveColor,
                                            )
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    enabled = tracks.isNotEmpty(),
                                    onClick = { play(details, tracks, 0) },
                                    modifier = Modifier.weight(1.4f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        disabledContainerColor = accent.copy(alpha = 0.3f),
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
                                    enabled = tracks.isNotEmpty(),
                                    onClick = { play(details, tracks.shuffled(), 0) },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent.copy(alpha = 0.12f),
                                        disabledContainerColor = accent.copy(alpha = 0.06f),
                                    ),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.shuffle),
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                    )
                                }
                            }

                            details.description?.let {
                                Text(
                                    text = it,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = adaptiveColor.copy(alpha = 0.7f),
                                )
                            }

                            details.licenseUrl?.let { license ->
                                Text(
                                    text = stringResource(R.string.ia_license) + ": " + license.substringAfter("//"),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent,
                                    modifier = Modifier.clickable { uriHandler.openUri(license) },
                                )
                            }

                            Text(
                                text = stringResource(R.string.ia_view_site),
                                style = MaterialTheme.typography.bodyMedium,
                                color = accent,
                                modifier = Modifier.clickable { uriHandler.openUri(details.item.detailsUrl) },
                            )
                        }
                    }

                    if (tracks.isEmpty()) {
                        item(key = "ia_item_no_audio") {
                            Text(
                                text = stringResource(R.string.ia_no_audio),
                                color = adaptiveColor.copy(alpha = 0.7f),
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    itemsIndexed(items = tracks, key = { _, track -> track.id }) { index, track ->
                        val isActive = mediaMetadata?.id == track.id
                        IaTrackRow(
                            track = track,
                            rankNumber = index + 1,
                            isActive = isActive,
                            isPlaying = isPlaying,
                            onClick = {
                                if (isActive) playerConnection.player.togglePlayPause()
                                else play(details, tracks, index)
                            },
                        )
                    }
                }
            }
        }
        IaTopBar(
            title = title,
            navController = navController,
            showTitle = showTopBarTitle,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}


@Composable
private fun IaTrackRow(
    track: IaTrack,
    rankNumber: Int,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val librarySong by database.song(track.id).collectAsState(initial = null)
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    ListItem(
        title = track.title,
        subtitle = listOfNotNull(
            track.artist.takeIf { it.isNotBlank() },
            track.durationSeconds?.let { makeTimeString(it * 1000L) },
        ).joinToString(" • "),
        badges = {
            if (librarySong?.song?.liked == true) {
                Icon(
                    painter = painterResource(R.drawable.favorite),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
                )
            }
        },
        thumbnailContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ListThumbnailSize),
            ) {
                ResultThumbnail(
                    url = track.thumbnailUrl,
                    rankNumber = rankNumber,
                    isActive = isActive,
                )
                PlayingIndicatorBox(
                    isActive = isActive,
                    playWhenReady = isPlaying,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                )
            }
        },
        trailingContent = {
            IconButton(
                onClick = {
                    menuState.show {
                        IaTrackMenu(
                            track = track,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = adaptiveColor,
                )
            }
        },
        isActive = isActive,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}