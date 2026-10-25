/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.betsy.music.utils.dataStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil.compose.AsyncImage
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.utils.completed
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalDownloadUtil
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.ListThumbnailSize
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.constants.ThumbnailCornerRadius
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.music.db.entities.PlaylistSongMap
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.models.MediaMetadata
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.ExoDownloadService
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.DefaultDialog
import com.betsy.music.ui.component.ListDialog
import com.betsy.music.ui.component.ListItem
import com.betsy.music.ui.component.StreamingListItem
import com.betsy.music.ui.component.MenuItemData
import com.betsy.music.ui.component.MenuGroup
import com.betsy.music.ui.component.NewAction
import com.betsy.music.ui.component.NewActionGrid
import com.betsy.music.utils.joinByBullet
import com.betsy.music.utils.makeTimeString
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun StreamingPlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)

    
    
    
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }
    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)
    val guestId   = rememberGuestId()
    val ownerId   = remember(prefs, guestId) {
        (prefs[activeAccountIdKey] ?: "").ifBlank { guestId }
    }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { targetPlaylist ->
            val allSongs = songs
                .ifEmpty {
                    AudiusApi.playlist(targetPlaylist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach { insert(it) }
            }
            coroutineScope.launch(Dispatchers.IO) {
                targetPlaylist.playlist.browseId?.let { playlistId ->
                    AudiusApi.addPlaylistToPlaylist(playlistId, targetPlaylist.id)
                }
            }
            allSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    StreamingListItem(
        item = playlist,
        trailingContent = {
            if (playlist.id != "LM" && !playlist.isEditable) {
                IconButton(
                    onClick = {
                        if (dbPlaylist?.playlist == null) {
                            database.transaction {
                                val playlistEntity = PlaylistEntity(
                                    name = playlist.title,
                                    browseId = playlist.id,
                                    isEditable = false,
                                    remoteSongCount = playlist.songCountText?.let {
                                        Regex("""\d+""").find(
                                            it
                                        )?.value?.toIntOrNull()
                                    },
                                    playEndpointParams = playlist.playEndpoint?.params,
                                    shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = playlist.radioEndpoint?.params,
                                    ownerId     = ownerId,
                                    isLocalMode = !isStreamingMode,
                                ).toggleLike()
                                insert(playlistEntity)
                                coroutineScope.launch(Dispatchers.IO) {
                                    songs.ifEmpty {
                                        AudiusApi.playlist(playlist.id).completed()
                                            .getOrNull()?.songs.orEmpty()
                                    }.map { it.toMediaMetadata() }
                                        .onEach { insert(it) }
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index
                                            )
                                        }
                                        .forEach { insert(it) }
                                }
                            }
                        } else {
                            database.transaction {
                                val toggled = dbPlaylist!!.playlist.toggleLike()
                                
                                update(
                                    if (toggled.bookmarkedAt != null) toggled.copy(ownerId = ownerId, isLocalMode = !isStreamingMode)
                                    else toggled
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null
                    )
                }
            }
        }
    )

    Spacer(modifier = Modifier.height(20.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED })
                    Download.STATE_COMPLETED
                else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED
                                || downloads[it.id]?.state == Download.STATE_DOWNLOADING
                                || downloads[it.id]?.state == Download.STATE_COMPLETED
                    })
                    Download.STATE_DOWNLOADING
                else
                    Download.STATE_STOPPED
        }
    }
    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs = songs
                .ifEmpty {
                    AudiusApi.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                }.map {
                    it.toMediaMetadata()
                }
            database.transaction {
                allSongs.forEach { insert(it) }
            }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        onDismiss = { showImportPlaylistDialog = false }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                ListItem(
                    title = song.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle =
                        joinByBullet(
                            song.artists.joinToString { it.name },
                            makeTimeString(song.duration * 1000L),
                        ),
                )
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        
        item {
            NewActionGrid(
                actions = buildList {
                    playlist.playEndpoint?.let {
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = stringResource(R.string.play),
                                onClick = {
                                    playerConnection.playQueue(StreamingQueue(it))
                                    onDismiss()
                                }
                            )
                        )
                    }

                    playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = stringResource(R.string.shuffle),
                                onClick = {
                                    playerConnection.playQueue(StreamingQueue(shuffleEndpoint))
                                    onDismiss()
                                }
                            )
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        
        item {
            MenuGroup(
                items = listOf(
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.play_next)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_play),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            coroutineScope.launch {
                                songs
                                    .ifEmpty {
                                        withContext(Dispatchers.IO) {
                                            AudiusApi
                                                .playlist(playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }
                                    }.let { songs ->
                                        playerConnection.playNext(songs.map { it.toMediaItem() })
                                    }
                            }
                            onDismiss()
                        }
                    ),
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.add_to_queue)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            coroutineScope.launch {
                                songs
                                    .ifEmpty {
                                        withContext(Dispatchers.IO) {
                                            AudiusApi
                                                .playlist(playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }
                                    }.let { songs ->
                                        playerConnection.addToQueue(songs.map { it.toMediaItem() })
                                    }
                            }
                            onDismiss()
                        }
                    ),
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.add_to_playlist)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { showChoosePlaylistDialog = true }
                    )
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        
        if (songs.isNotEmpty()) {
            item {
                MenuGroup(
                    items = listOf(
                        when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.remove_download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
                                )
                            }

                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    icon = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
                                )
                            }

                            else -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = {
                                        songs.forEach { song ->
                                            val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                                                .setCustomCacheKey(song.id)
                                                .setData(song.title.toByteArray())
                                                .build()
                                            DownloadService.sendAddDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                downloadRequest,
                                                false
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        
        item {
            MenuGroup(
                items = buildList {
                    if (canSelect) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.select)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.select_all),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    onDismiss()
                                    selectAction()
                                }
                            )
                        )
                    }

                    add(
                        MenuItemData(
                            title = { Text(text = stringResource(R.string.share)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.share),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                                onDismiss()
                            }
                        )
                    )
                }
            )
        }
    }
}