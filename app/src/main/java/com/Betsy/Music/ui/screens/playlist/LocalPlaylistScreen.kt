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
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.audius.compat.AudiusApi
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalDownloadUtil
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.PlaylistEditLockKey
import com.betsy.music.constants.PlaylistSongSortDescendingKey
import com.betsy.music.constants.PlaylistSongSortType
import com.betsy.music.constants.PlaylistSongSortTypeKey
import com.betsy.music.db.entities.Playlist
import com.betsy.music.db.entities.PlaylistSong
import com.betsy.music.db.entities.PlaylistSongMap
import com.betsy.music.extensions.move
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.ExoDownloadService
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.ui.component.DefaultDialog
import com.betsy.music.ui.component.DraggableScrollbar
import com.betsy.music.ui.component.EmptyPlaceholder
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.SortHeader
import com.betsy.music.ui.component.TextFieldDialog
import com.betsy.music.ui.menu.SelectionSongMenu
import com.betsy.music.ui.utils.ItemWrapper
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.utils.deletePlaylistImage
import com.betsy.music.utils.toMediaMetadata as iaToMediaMetadata
import com.betsy.music.utils.getPlaylistImageUri
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import com.betsy.music.utils.saveCustomPlaylistImage
import com.betsy.music.viewmodels.LocalPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.betsy.music.ui.menu.AddSongsToPlaylistSheet
import java.time.LocalDateTime
import com.betsy.music.db.entities.Song as SongEntity

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import com.betsy.music.constants.StreamingModeKey
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.derivedStateOf
import com.betsy.music.ui.theme.rememberAdaptiveOnColor


private val PLTextW: Color
    @Composable get() = rememberAdaptiveOnColor()
private val PLTextWDim: Color
    @Composable get() = rememberAdaptiveOnColor().copy(alpha = 0.55f)
private val PLRed      = Color(0xFFFF5252)
private val PLDivider: Color
    @Composable get() = rememberAdaptiveOnColor().copy(alpha = 0.07f)


private data class PLColors(
    val card   : Color,
    val cardAlt: Color,
    val menuBg : Color,
)

@Composable
private fun rememberPLColors(): PLColors {
    val (accentInt, _) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accent = remember(accentInt) { Color(accentInt.toLong() or 0xFF000000L) }

    return remember(accent) {
        PLColors(
            card    = accent.copy(alpha = 0.10f),
            cardAlt = accent.copy(alpha = 0.18f),
            menuBg  = Color(
                red   = (accent.red   * 0.12f).coerceAtMost(1f),
                green = (accent.green * 0.12f).coerceAtMost(1f),
                blue  = (accent.blue  * 0.12f).coerceAtMost(1f),
                alpha = 0.97f,
            ),
        )
    }
}


@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController : NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel     : LocalPlaylistViewModel = hiltViewModel(),
) {
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accent = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val plColors          = rememberPLColors()
    val context          = LocalContext.current
    val menuState        = LocalMenuState.current
    val database         = LocalDatabase.current
    val haptic           = LocalHapticFeedback.current
    val focusManager     = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying        by playerConnection.isPlaying.collectAsState()
    val mediaMetadata    by playerConnection.mediaMetadata.collectAsState()

    val playlist       by viewModel.playlist.collectAsState()
    val songs          by viewModel.playlistSongs.collectAsState()
    val mutableSongs   = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength = remember(songs) { songs.fastSumBy { it.song.song.duration } }

    val (sortType, onSortTypeChange)             = rememberEnumPreference(PlaylistSongSortTypeKey, PlaylistSongSortType.CUSTOM)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSongSortDescendingKey, true)
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)

    val coroutineScope    = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query       by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs
        else songs.filter {
            it.song.song.title.contains(query.text, ignoreCase = true) ||
                    it.song.artists.fastAny { a -> a.name.contains(query.text, ignoreCase = true) }
        }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }
    if (isSearching) BackHandler { isSearching = false; query = TextFieldValue() }

    val wrappedSongs = filteredSongs.map { ItemWrapper(it) }.toMutableList()
    var selection by remember { mutableStateOf(false) }
    if (selection) BackHandler { selection = false }

    val downloadUtil  = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val editable: Boolean = playlist?.playlist?.isEditable == true

    var customThumbnailUri by rememberSaveable {
        mutableStateOf<Uri?>(playlist?.playlist?.id?.let { getPlaylistImageUri(context, it) })
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            customThumbnailUri = it
            playlist?.playlist?.id?.let { id -> saveCustomPlaylistImage(context, id, it) }
        }
    }

    LaunchedEffect(songs) {
        mutableSongs.apply { clear(); addAll(songs) }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = when {
                songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } -> Download.STATE_COMPLETED
                songs.all {
                    downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                } -> Download.STATE_DOWNLOADING
                else -> Download.STATE_STOPPED
            }
        }
    }

    var showEditDialog           by remember { mutableStateOf(false) }
    var showEditSheet            by remember { mutableStateOf(false) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var dismissJob: Job?         by remember { mutableStateOf(null) }

    if (showEditDialog) {
        playlist?.playlist?.let { pe ->
            TextFieldDialog(
                icon  = { Icon(painterResource(R.drawable.edit), null) },
                title = { Text(stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(pe.name, TextRange(pe.name.length)),
                onDone = { name ->
                    database.query { update(pe.copy(name = name, lastUpdateTime = LocalDateTime.now())) }
                    viewModel.viewModelScope.launch(Dispatchers.IO) { pe.browseId?.let { AudiusApi.renamePlaylist(it, name) } }
                }
            )
        }
    }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = { Text(stringResource(R.string.remove_download_playlist_confirm, playlist?.playlist!!.name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showRemoveDownloadDialog = false
                    if (!editable) database.transaction { playlist?.id?.let { clearPlaylist(it) } }
                    songs.forEach { DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, it.song.id, false) }
                }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = { Text(stringResource(R.string.delete_playlist_confirm, playlist?.playlist!!.name), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 18.dp)) },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = {
                    showDeletePlaylistDialog = false
                    database.query { playlist?.let { delete(it.playlist) } }
                    viewModel.viewModelScope.launch(Dispatchers.IO) { playlist?.playlist?.browseId?.let { AudiusApi.deletePlaylist(it) } }
                    navController.popBackStack()
                }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    val headerItems   = 2
    val lazyListState = rememberLazyListState()
    var dragInfo      by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var topBarHeight by remember { mutableStateOf(56.dp) }
    val density = LocalDensity.current

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState          = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            val cur = dragInfo
            dragInfo = if (cur == null) (from.index - headerItems) to (to.index - headerItems)
            else cur.first to (to.index - headerItems)
            mutableSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction { move(viewModel.playlistId, from, to) }
                if (viewModel.playlist.value?.playlist?.browseId != null) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val maps    = database.playlistSongMaps(viewModel.playlistId, 0)
                        val succIdx = if (from > to) to else to + 1
                        val succSv  = maps.getOrNull(succIdx)?.setVideoId
                        maps.getOrNull(from)?.setVideoId?.let { svid ->
                            AudiusApi.moveSongPlaylist(viewModel.playlist.value?.playlist?.browseId!!, svid, succSv.toString())
                        }
                    }
                }
                dragInfo = null
            }
        }
    }

    
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {

        LazyColumn(
            state          = lazyListState,
            contentPadding = PaddingValues(

                bottom = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                    .asPaddingValues().calculateBottomPadding()
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight)  
        ) {
            playlist?.let { pl ->
                if (pl.songCount == 0 && pl.playlist.remoteSongCount == 0) {
                    item(key = "empty") {
                        EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty), modifier = Modifier)
                    }
                } else {
                    if (!isSearching) {
                        item(key = "header") {
                            PLHeader(
                                playlist           = pl,
                                songs              = songs,
                                customThumbnailUri = customThumbnailUri,
                                plColors           = plColors,
                                onPickImage        = { imagePickerLauncher.launch("image/*") },
                                onRemoveImage      = {
                                    pl.playlist.id.let { deletePlaylistImage(context, it) }
                                    customThumbnailUri = null
                                },
                                onPlayAll  = { playerConnection.playQueue(ListQueue(title = pl.playlist.name, items = songs.map { it.song.toSafeMediaItem() })) },
                                onShuffle  = { playerConnection.playQueue(ListQueue(title = pl.playlist.name, items = songs.shuffled().map { it.song.toSafeMediaItem() })) }
                            )
                        }
                    }

                    item(key = "controls") {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            SortHeader(
                                sortType               = sortType,
                                sortDescending         = sortDescending,
                                onSortTypeChange       = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText           = { type ->
                                    when (type) {
                                        PlaylistSongSortType.CUSTOM      -> R.string.sort_by_custom
                                        PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        PlaylistSongSortType.NAME        -> R.string.sort_by_name
                                        PlaylistSongSortType.ARTIST      -> R.string.sort_by_artist
                                        PlaylistSongSortType.PLAY_TIME   -> R.string.sort_by_play_time
                                    }
                                }
                            )
                            if (editable) {
                                FilledIconToggleButton(
                                    checked         = locked,
                                    onCheckedChange = { locked = it },
                                    colors          = IconButtonDefaults.filledIconToggleButtonColors(
                                        containerColor        = PLTextW.copy(alpha = 0.08f),
                                        contentColor          = accent.copy(alpha = 0.6f),
                                        checkedContainerColor = accent.copy(alpha = 0.2f),
                                        checkedContentColor   = accent
                                    )
                                ) {
                                    Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null, tint = if (locked) accent else PLTextW.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }

                    if (!selection) {
                        itemsIndexed(
                            items = if (isSearching) filteredSongs else mutableSongs,
                            key   = { _, s -> s.map.id }
                        ) { index, song ->
                            ReorderableItem(state = reorderableState, key = song.map.id) { isDragging ->
                                val currentItem by rememberUpdatedState(song)
                                val isLast   = index == (if (isSearching) filteredSongs.size else mutableSongs.size) - 1
                                val isActive = song.song.id == mediaMetadata?.id
                                var isVisible by remember { mutableStateOf(true) }
                                val canDrag  = sortType == PlaylistSongSortType.CUSTOM && !locked && !isSearching

                                @SuppressLint("StringFormatInvalid")
                                fun deleteFromPlaylist() {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        database.transaction {
                                            launch {
                                                pl.playlist.browseId?.let { browseId ->
                                                    val svid = getSetVideoId(currentItem.map.songId)
                                                    if (svid?.setVideoId != null)
                                                        AudiusApi.removeFromPlaylist(browseId, currentItem.map.songId, svid.setVideoId!!)
                                                }
                                            }
                                            move(currentItem.map.playlistId, currentItem.map.position, Int.MAX_VALUE)
                                            delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                        }
                                        dismissJob?.cancel()
                                        dismissJob = coroutineScope.launch {
                                            val r = snackbarHostState.showSnackbar(
                                                message     = context.getString(R.string.removed_song_from_playlist, currentItem.song.song.title),
                                                actionLabel = context.getString(R.string.undo),
                                                duration    = SnackbarDuration.Short
                                            )
                                            if (r == SnackbarResult.ActionPerformed) {
                                                database.transaction {
                                                    insert(currentItem.map.copy(position = playlistLength))
                                                    move(currentItem.map.playlistId, playlistLength, currentItem.map.position)
                                                }
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = isVisible, exit = shrinkVertically(tween(300)) + fadeOut(tween(300))) {
                                    PLSongRow(
                                        index              = index + 1,
                                        song               = song,
                                        isActive           = isActive,
                                        isPlaying          = isPlaying,
                                        isLast             = isLast,
                                        isDragging         = isDragging,
                                        dragHandleModifier = if (canDrag) Modifier.draggableHandle() else Modifier,
                                        showDragHandle     = canDrag,
                                        plColors           = plColors,
                                        onRemove           = { deleteFromPlaylist() },
                                        onMoveUp           = if (index > 0) ({
                                            mutableSongs.move(index, index - 1)
                                            coroutineScope.launch { database.transaction { move(viewModel.playlistId, index, index - 1) } }
                                        }) else null,
                                        onMoveDown         = if (index < mutableSongs.lastIndex) ({
                                            mutableSongs.move(index, index + 1)
                                            coroutineScope.launch { database.transaction { move(viewModel.playlistId, index, index + 1) } }
                                        }) else null,
                                        onClick = {
                                            if (song.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                            else playerConnection.playQueue(
                                                ListQueue(title = pl.playlist.name, items = songs.map { it.song.toSafeMediaItem() }, startIndex = songs.indexOfFirst { it.map.id == song.map.id })
                                            )
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            wrappedSongs.find { it.item.map.id == song.map.id }?.isSelected = true
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(wrappedSongs, key = { _, s -> s.item.map.id }) { index, sw ->
                            ReorderableItem(state = reorderableState, key = sw.item.map.id) { _ ->
                                val currentItem by rememberUpdatedState(sw.item)
                                val isLast   = index == wrappedSongs.size - 1
                                val isActive = sw.item.song.id == mediaMetadata?.id
                                var isVisible by remember { mutableStateOf(true) }

                                AnimatedVisibility(visible = isVisible, exit = shrinkVertically(tween(300)) + fadeOut(tween(300))) {
                                    PLSongRow(
                                        index              = index + 1,
                                        song               = sw.item,
                                        isActive           = isActive,
                                        isPlaying          = isPlaying,
                                        isLast             = isLast,
                                        isDragging         = false,
                                        dragHandleModifier = Modifier,
                                        showDragHandle     = false,
                                        isSelected         = sw.isSelected && selection,
                                        plColors           = plColors,
                                        onRemove           = {
                                            isVisible = false
                                            coroutineScope.launch {
                                                delay(300)
                                                database.transaction {
                                                    move(currentItem.map.playlistId, currentItem.map.position, Int.MAX_VALUE)
                                                    delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                                }
                                                wrappedSongs.remove(sw)
                                            }
                                        },
                                        onMoveUp   = null,
                                        onMoveDown = null,
                                        onClick = {
                                            if (!selection) {
                                                if (sw.item.song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                                else playerConnection.playQueue(ListQueue(title = pl.playlist.name, items = songs.map { it.song.toSafeMediaItem() }, startIndex = index))
                                            } else sw.isSelected = !sw.isSelected
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            sw.isSelected = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item(key = "end") { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        DraggableScrollbar(
            modifier    = Modifier.padding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues()).align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 2
        )

        
        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(pluralStringResource(R.plurals.n_song, count, count), color = PLTextW)
                    }
                    isSearching -> {
                        TextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.search), color = PLTextWDim) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = PLTextW),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                    else -> {
                        if (!transparentAppBar) {
                            Text(
                                text = playlist?.playlist?.name.orEmpty(),
                                color = PLTextW,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                
                com.betsy.music.ui.component.IconButton(
                    onClick     = {
                        when {
                            isSearching -> { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }
                            selection   -> selection = false
                            else        -> navController.navigateUp()
                        }
                    },
                    onLongClick = { if (!isSearching && !selection) navController.backToMain() }
                ) {
                    Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null, tint = PLTextW)
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(onClick = {
                        if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false }
                        else wrappedSongs.forEach { it.isSelected = true }
                    }) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null, tint = PLTextW) }
                    IconButton(onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }.map { it.item.song },
                                songPosition  = wrappedSongs.filter { it.isSelected }.map { it.item.map },
                                onDismiss     = menuState::dismiss,
                                clearAction   = { selection = false; wrappedSongs.clear() }
                            )
                        }
                    }) { Icon(painterResource(R.drawable.more_vert), null, tint = PLTextW) }
                } else if (!isSearching) {
                    
                    IconButton(onClick = { isSearching = true }) {
                        Icon(painterResource(R.drawable.search), null, tint = PLTextW)
                    }
                    
                    Box(
                        modifier = Modifier.padding(end = 8.dp).size(40.dp).clip(CircleShape)
                            .background(accent).clickable { showEditSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.edit), null, tint = Color(0xFF071520), modifier = Modifier.size(20.dp))
                    }
                }
            },
            
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            windowInsets   = TopAppBarDefaults.windowInsets,

            modifier       = Modifier.onGloballyPositioned { coords ->
                topBarHeight = with(density) { coords.size.height.toDp() }
            }
        )



        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)).align(Alignment.BottomCenter)
        )

        if (showEditSheet) {
            EditPlaylistSheet(playlistId = viewModel.playlistId, playlistSongs = songs, onDismiss = { showEditSheet = false })
        }
    }
}




@Composable
private fun PLHeader(
    playlist          : Playlist,
    songs             : List<PlaylistSong>,
    customThumbnailUri: Uri?,
    plColors          : PLColors,
    onPickImage       : () -> Unit,
    onRemoveImage     : () -> Unit,
    onPlayAll         : () -> Unit,
    onShuffle         : () -> Unit,
) {
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accent = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val thumbModel: Any? = customThumbnailUri ?: if (playlist.thumbnails.isNotEmpty()) playlist.thumbnails[0] else null

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                playlist.playlist.name,
                color = PLTextW, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount))
                    if ((playlist.playlist.remoteSongCount ?: 0) > 0)
                        append(" (${playlist.playlist.remoteSongCount} online)")
                },
                color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium
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
                    .background(plColors.card)
                    .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                when {
                    thumbModel != null -> AsyncImage(
                        model = thumbModel, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                    playlist.thumbnails.size > 1 -> {
                        val tl = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
                        Box(Modifier.fillMaxSize()) {
                            tl.forEachIndexed { i, alignment ->
                                AsyncImage(
                                    model = playlist.thumbnails.getOrNull(i), contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.align(alignment).fillMaxSize(0.5f)
                                )
                            }
                        }
                    }
                    else -> Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(plColors.cardAlt, plColors.card))
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.queue_music), null, tint = accent.copy(alpha = 0.3f), modifier = Modifier.size(72.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))
                ))

                Row(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (customThumbnailUri != null) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onRemoveImage),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Close, null, tint = PLTextW, modifier = Modifier.size(16.dp)) }
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(accent.copy(alpha = 0.92f))
                            .clickable(onClick = onPickImage)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFF071520), modifier = Modifier.size(13.dp))
                            Text(stringResource(R.string.customize_image), color = Color(0xFF071520), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick  = onPlayAll,
                modifier = Modifier.weight(1.4f).height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(painterResource(R.drawable.play), null, tint = Color(0xFF071520), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.play), color = Color(0xFF071520), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Button(
                onClick  = onShuffle,
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.12f)),
                border   = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
            ) {
                Icon(painterResource(R.drawable.shuffle), null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.shuffle), color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = PLDivider, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
    }
}




@Composable
private fun PlayingEqualizer(
    color   : Color,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_transition")
    val bars = List(barCount) { i ->
        val duration = 480 + i * 140
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue  = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "eq_bar_$i",
        )
    }

    Row(
        modifier              = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment     = Alignment.Bottom,
    ) {
        bars.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(anim.value)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}




@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PLSongRow(
    index              : Int,
    song               : PlaylistSong,
    isActive           : Boolean,
    isPlaying          : Boolean,
    isLast             : Boolean,
    isDragging         : Boolean,
    dragHandleModifier : Modifier,
    showDragHandle     : Boolean,
    isSelected         : Boolean = false,
    plColors           : PLColors,
    onRemove           : () -> Unit,
    onMoveUp           : (() -> Unit)?,
    onMoveDown         : (() -> Unit)?,
    onClick            : () -> Unit,
    onLongClick        : () -> Unit,
) {
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accent = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    var showDotMenu by remember { mutableStateOf(false) }

    val bg = when {
        isDragging -> plColors.cardAlt
        isSelected -> accent.copy(alpha = 0.14f)
        isActive   -> accent.copy(alpha = 0.15f)
        else       -> Color.Transparent
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(
                    width = if (isActive) 1.5.dp else 0.dp,
                    color = if (isActive) accent else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 12.dp, end = 0.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) {
                val t = song.song.song.thumbnailUrl
                if (t != null)
                    AsyncImage(t, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else
                    Box(
                        modifier = Modifier.fillMaxSize().background(plColors.cardAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.music_note), null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    }

                if (isActive && isPlaying) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        
                        PlayingEqualizer(color = accent)
                    }
                } else if (!isActive) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            index.toString(),
                            color      = Color.White,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.song.song.title,
                    color      = if (isActive) accent.copy(alpha = 0.85f) else PLTextW,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    song.song.artists.joinToString { it.name },
                    color    = PLTextWDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            
            Box {
                IconButton(onClick = { showDotMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(R.drawable.more_vert), null, tint = PLTextWDim)
                }
                DropdownMenu(
                    expanded         = showDotMenu,
                    onDismissRequest = { showDotMenu = false },
                    containerColor   = plColors.menuBg,
                    shape            = RoundedCornerShape(14.dp),
                    modifier         = Modifier.width(210.dp)
                ) {
                    if (onMoveUp != null) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.KeyboardArrowUp, null, tint = accent, modifier = Modifier.size(18.dp))
                                    }
                                    Text(stringResource(R.string.move_up_in_playlist), color = PLTextW, fontSize = 14.sp)
                                }
                            },
                            onClick = { showDotMenu = false; onMoveUp() }
                        )
                    }
                    if (onMoveDown != null) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = accent, modifier = Modifier.size(18.dp))
                                    }
                                    Text(stringResource(R.string.move_down_in_playlist), color = PLTextW, fontSize = 14.sp)
                                }
                            },
                            onClick = { showDotMenu = false; onMoveDown() }
                        )
                    }
                    if (onMoveUp != null || onMoveDown != null) {
                        HorizontalDivider(color = PLDivider, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(PLRed.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, null, tint = PLRed, modifier = Modifier.size(18.dp))
                                }
                                Text(stringResource(R.string.remove_from_playlist), color = PLRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        },
                        onClick = { showDotMenu = false; onRemove() }
                    )
                }
            }

            if (showDragHandle) {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp).then(dragHandleModifier)) {
                    Icon(painterResource(R.drawable.drag_handle), null, tint = PLTextWDim.copy(alpha = 0.5f))
                }
            }
        }
        if (!isLast) Spacer(Modifier.height(4.dp))
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistSheet(
    playlistId   : String,
    playlistSongs: List<PlaylistSong>,
    onDismiss    : () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()
    val plColors   = rememberPLColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = plColors.menuBg,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f)))
        }
    ) {
        EditPlaylistContent(
            playlistId    = playlistId,
            playlistSongs = playlistSongs,
            plColors      = plColors,
            onClose       = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
        )
    }
}

@Composable
private fun EditPlaylistContent(
    playlistId   : String,
    playlistSongs: List<PlaylistSong>,
    plColors     : PLColors,
    onClose      : () -> Unit,
) {
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accent = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    var activeTab    by remember { mutableIntStateOf(0) }
    val database     = LocalDatabase.current
    val scope        = rememberCoroutineScope()
    val editList     = remember(playlistSongs) { mutableStateListOf<PlaylistSong>().apply { addAll(playlistSongs) } }
    val librarySongs = remember { mutableStateListOf<SongEntity>() }

    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.edit_playlist), color = PLTextW, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = PLTextW) }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(14.dp)).background(plColors.card)) {
            listOf(
                stringResource(R.string.edit) + " (${editList.size})" to R.drawable.edit,
                stringResource(R.string.add_to_library) to R.drawable.add
            ).forEachIndexed { i, (label, icon) ->
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (activeTab == i) accent else Color.Transparent)
                        .clickable { activeTab = i }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(icon), null, tint = if (activeTab == i) Color(0xFF071520) else accent.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Text(label, color = if (activeTab == i) Color(0xFF071520) else PLTextWDim, fontSize = 13.sp, fontWeight = if (activeTab == i) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (activeTab) {
            0 -> EditTab(
                songs      = editList,
                plColors   = plColors,
                onMoveUp   = { idx ->
                    if (idx > 0) { editList.add(idx - 1, editList.removeAt(idx)); scope.launch { database.transaction { move(playlistId, idx, idx - 1) } } }
                },
                onMoveDown = { idx ->
                    if (idx < editList.lastIndex) { editList.add(idx + 1, editList.removeAt(idx)); scope.launch { database.transaction { move(playlistId, idx, idx + 1) } } }
                },
                onRemove   = { song ->
                    editList.remove(song)
                    scope.launch { database.transaction { move(song.map.playlistId, song.map.position, Int.MAX_VALUE); delete(song.map.copy(position = Int.MAX_VALUE)) } }
                }
            )
            1 -> AddSongsTab(
                playlistId        = playlistId,
                alreadyInPlaylist = editList.map { it.song.id }.toSet(),
                plColors          = plColors,
            )
        }
    }
}


@Composable
private fun EditTab(
    songs     : List<PlaylistSong>,
    plColors  : PLColors,
    onMoveUp  : (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove  : (PlaylistSong) -> Unit,
) {
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accent = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val lazyState   = rememberLazyListState()
    val reorderable = rememberReorderableLazyListState(lazyListState = lazyState) { from, to ->
        (songs as? MutableList<PlaylistSong>)?.apply { add(to.index, removeAt(from.index)) }
    }
    val visMap = remember(songs) { songs.associate { it.map.id to mutableStateOf(true) } }

    LazyColumn(state = lazyState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(songs, key = { _, s -> s.map.id }) { index, song ->
            val vis = visMap[song.map.id] ?: remember { mutableStateOf(true) }
            ReorderableItem(state = reorderable, key = song.map.id) { isDragging ->
                AnimatedVisibility(visible = vis.value, exit = shrinkVertically(tween(250)) + fadeOut(tween(250))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDragging) plColors.cardAlt else plColors.card)
                            .border(width = if (isDragging) 1.dp else 0.dp, color = if (isDragging) accent.copy(alpha = 0.5f) else Color.Transparent, shape = RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { onMoveUp(index) }, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, null, tint = if (index > 0) accent.copy(alpha = 0.6f) else accent.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onMoveDown(index) }, enabled = index < songs.lastIndex, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = if (index < songs.lastIndex) accent.copy(alpha = 0.6f) else accent.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(plColors.cardAlt)) {
                            val t = song.song.song.thumbnailUrl
                            if (t != null) AsyncImage(t, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else Icon(painterResource(R.drawable.music_note), null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(24.dp).align(Alignment.Center))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.song.song.title, color = PLTextW, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.song.artists.joinToString { it.name }, color = PLTextWDim, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp).draggableHandle()) {
                            Icon(painterResource(R.drawable.drag_handle), null, tint = accent.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { vis.value = false; onRemove(song) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Remove, null, tint = PLRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}


@Composable
private fun AddSongsTab(
    playlistId        : String,
    alreadyInPlaylist : Set<String>,
    plColors          : PLColors,
) {
    val context          = LocalContext.current
    val database         = LocalDatabase.current
    val scope            = rememberCoroutineScope()
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accent           = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)
    var isStreaming by remember { mutableStateOf(isStreamingMode) }
    LaunchedEffect(isStreamingMode) { isStreaming = isStreamingMode }

    var query            by remember { mutableStateOf("") }
    var streamingResults by remember { mutableStateOf<List<com.betsy.music.models.MediaMetadata>>(emptyList()) }
    var localResults     by remember { mutableStateOf<List<LocalDeviceSong>>(emptyList()) }
    var isLoading        by remember { mutableStateOf(false) }
    var searchJob        by remember { mutableStateOf<Job?>(null) }

    var playlistSongIds by remember { mutableStateOf<Set<String>>(alreadyInPlaylist) }
    LaunchedEffect(playlistId) {
        database.playlistSongs(playlistId).collect { songs ->
            playlistSongIds = songs.map { it.map.songId }.toSet()
        }
    }

    var playlist by remember { mutableStateOf<com.betsy.music.db.entities.Playlist?>(null) }
    LaunchedEffect(playlistId) {
        database.playlist(playlistId).collect { playlist = it }
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val hasAudioPermission = remember {
        context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun searchLocal(q: String): List<LocalDeviceSong> = withContext(Dispatchers.IO) {
        if (!hasAudioPermission || q.length < 2) return@withContext emptyList()
        val list = mutableListOf<LocalDeviceSong>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
        )
        
        
        val words = q.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return@withContext emptyList()
        val wordCond = words.joinToString(" AND ") {
            "(LOWER(REPLACE(${MediaStore.Audio.Media.TITLE}, '''', '')) LIKE ? " +
                    "OR LOWER(REPLACE(${MediaStore.Audio.Media.ARTIST}, '''', '')) LIKE ? " +
                    "OR LOWER(REPLACE(${MediaStore.Audio.Media.ALBUM}, '''', '')) LIKE ?)"
        }
        val sel  = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000 AND $wordCond"
        val args = words.flatMap { listOf("%$it%", "%$it%", "%$it%") }.toTypedArray()
        context.contentResolver.query(collection, projection, sel, args, "${MediaStore.Audio.Media.TITLE} ASC")
            ?.use { cursor ->
                val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val sid = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    list.add(LocalDeviceSong(
                        id = sid, title = cursor.getString(titleCol) ?: context.getString(R.string.unknown),
                        artist = cursor.getString(artistCol) ?: context.getString(R.string.unknown_artist),
                        album = cursor.getString(albumCol) ?: "",
                        duration = cursor.getLong(durCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, sid),
                        albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId),
                    ))
                }
            }
        list
    }

    
    suspend fun searchStreaming(q: String): List<com.betsy.music.models.MediaMetadata> {
        if (q.length < 2) return emptyList()
        return withContext(Dispatchers.IO) {
            coroutineScope {
                val audiusJob = async {
                    runCatching {
                        AudiusApi.search(q, AudiusApi.SearchFilter.FILTER_SONG)
                            .getOrNull()
                            ?.items
                            ?.filterIsInstance<com.betsy.audius.compat.models.SongItem>()
                            ?.map { it.toMediaMetadata() }
                            ?: emptyList()
                    }.onFailure { e ->
                        android.util.Log.e("AddSongsTab", "audius error: ${e.message}")
                    }.getOrDefault(emptyList())
                }

                
                
                val iaJob = async {
                    runCatching {
                        val page = InternetArchive.client.search(q, page = 1, rows = 5).getOrNull()
                        page?.items.orEmpty()
                            .map { item ->
                                async { InternetArchive.client.details(item.identifier).getOrNull()?.tracks.orEmpty() }
                            }
                            .awaitAll()
                            .flatten()
                            .take(40)
                            .map { it.iaToMediaMetadata() }
                    }.onFailure { e ->
                        android.util.Log.e("AddSongsTab", "internet archive error: ${e.message}")
                    }.getOrDefault(emptyList())
                }

                (audiusJob.await() + iaJob.await()).distinctBy { it.id }
            }
        }
    }

    LaunchedEffect(query, isStreaming) {
        searchJob?.cancel()
        if (query.length < 2) { streamingResults = emptyList(); localResults = emptyList(); isLoading = false; return@LaunchedEffect }
        searchJob = scope.launch {
            delay(400)
            isLoading = true
            if (isStreaming) { streamingResults = searchStreaming(query); localResults = emptyList() }
            else             { localResults = searchLocal(query); streamingResults = emptyList() }
            isLoading = false
        }
    }

    fun addStreamingSong(meta: com.betsy.music.models.MediaMetadata) {
        val pl = playlist ?: return
        scope.launch(Dispatchers.IO) {
            try { database.insert(meta) } catch (e: Exception) {
                android.util.Log.e("AddSongsTab", "insert error: ${e.message}")
            }
            database.addSongToPlaylist(pl, listOf(meta.id))
        }
    }

    fun addLocalSong(song: LocalDeviceSong) {
        val mediaId = "device_${song.id}"
        scope.launch(Dispatchers.IO) {
            try {
                val pl = playlist ?: database.playlist(playlistId).first()
                if (pl == null) {
                    android.util.Log.e("AddSongsTab", "playlist null para id=$playlistId")
                    return@launch
                }
                val songEntity = com.betsy.music.db.entities.SongEntity(
                    id           = mediaId,
                    title        = song.title,
                    duration     = (song.duration / 1000).toInt(),
                    thumbnailUrl = song.albumArtUri?.toString(),
                    albumName    = song.album.takeIf { it.isNotBlank() },
                )
                database.transaction {
                    insert(songEntity)
                    addSongToPlaylist(pl, listOf(mediaId))
                }
                android.util.Log.d("AddSongsTab", "local song added: $mediaId to ${pl.id}")
            } catch (e: Exception) {
                android.util.Log.e("AddSongsTab", "addLocalSong error: ${e.message}", e)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp)).background(plColors.card)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = PLTextW, fontSize = 15.sp),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(
                        if (isStreaming) "Buscar en streaming…" else "Buscar en tu dispositivo…",
                        color = PLTextWDim, fontSize = 15.sp,
                    )
                    inner()
                },
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accent, strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isStreaming) {
                items(streamingResults, key = { "st_${it.id}" }) { meta ->
                    val inPl = meta.id in playlistSongIds
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(plColors.card).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(plColors.cardAlt)) {
                            if (!meta.thumbnailUrl.isNullOrEmpty())
                                AsyncImage(meta.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else Icon(painterResource(R.drawable.music_note), null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(24.dp).align(Alignment.Center))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(meta.title, color = PLTextW, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(meta.artists.joinToString(", ") { it.name }, color = PLTextWDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { if (inPl) { scope.launch(Dispatchers.IO) { val maps = database.playlistSongs(playlistId).first(); maps.filter { it.map.songId == meta.id }.forEach { database.transaction { delete(it.map) } } } } else addStreamingSong(meta) }, modifier = Modifier.size(36.dp)) {
                            Icon(if (inPl) Icons.Default.Check else Icons.Default.Add, null, tint = if (inPl) accent else accent.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (!isStreaming) {
                items(localResults, key = { "loc_${it.id}" }) { song ->
                    val mediaId = "device_${song.id}"; val inPl = mediaId in playlistSongIds
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(plColors.card).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(plColors.cardAlt)) {
                            if (song.albumArtUri != null)
                                AsyncImage(song.albumArtUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else Icon(painterResource(R.drawable.music_note), null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(24.dp).align(Alignment.Center))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = PLTextW, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = PLTextWDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { if (inPl) { scope.launch(Dispatchers.IO) { val maps = database.playlistSongs(playlistId).first(); maps.filter { it.map.songId == mediaId }.forEach { database.transaction { delete(it.map) } } } } else addLocalSong(song) }, modifier = Modifier.size(36.dp)) {
                            Icon(if (inPl) Icons.Default.Check else Icons.Default.Add, null, tint = if (inPl) accent else accent.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            if (query.length >= 2 && !isLoading && streamingResults.isEmpty() && localResults.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_songs_found), color = PLTextWDim, fontSize = 14.sp) } }
            }
            if (query.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text(if (isStreaming) stringResource(R.string.type_to_search_streaming) else stringResource(R.string.type_to_search_phone), color = PLTextWDim.copy(alpha = 0.5f), fontSize = 13.sp) } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}


private data class LocalDeviceSong(
    val id: Long, val title: String, val artist: String,
    val album: String, val duration: Long, val uri: Uri, val albumArtUri: Uri?,
)




private fun com.betsy.music.db.entities.Song.toSafeMediaItem(): MediaItem {
    return if (song.id.startsWith("device_")) {
        val numericId = song.id.removePrefix("device_").toLongOrNull() ?: 0L
        val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, numericId.toString())
        val musicMeta = com.betsy.music.models.MediaMetadata(
            id           = song.id,
            title        = song.title,
            artists      = artists.map { com.betsy.music.models.MediaMetadata.Artist(id = null, name = it.name) },
            album        = song.albumName?.let { com.betsy.music.models.MediaMetadata.Album(id = "", title = it) },
            duration     = song.duration,
            thumbnailUrl = song.thumbnailUrl,
        )
        val extras = android.os.Bundle().apply {
            putString("mediaId", song.id)
            putBoolean("isLocal", true)
        }
        MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(uri)
            .setCustomCacheKey(song.id)
            .setTag(musicMeta)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).setExtras(extras).build())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(artists.joinToString { it.name }).setAlbumTitle(song.albumName).setArtworkUri(song.thumbnailUrl?.let { Uri.parse(it) }).build())
            .build()
    } else toMediaItem()
}