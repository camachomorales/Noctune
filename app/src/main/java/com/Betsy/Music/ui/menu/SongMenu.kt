/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.menu

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.audius.compat.AudiusApi
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalDownloadUtil
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.LocalSyncUtils
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.ListItemHeight
import com.betsy.music.constants.ListThumbnailSize
import com.betsy.music.db.entities.Event
import com.betsy.music.db.entities.PlaylistSong
import com.betsy.music.db.entities.Song
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.ExoDownloadService
import com.betsy.music.ui.component.GridMenu
import com.betsy.music.ui.component.GridMenuItem
import com.betsy.music.ui.component.ListDialog
import com.betsy.music.ui.component.LocalBottomSheetPageState
import com.betsy.music.ui.component.SongListItem
import com.betsy.music.ui.component.TextFieldDialog
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current.getDownload(originalSong.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val format by database.format(song.id).collectAsState(initial = null)
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_song)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue = TextFieldValue(song.song.title, TextRange(song.song.title.length)),
            onDone = { title ->
                onDismiss()
                database.query { update(song.song.copy(title = title)) }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { AudiusApi.addToPlaylist(it, song.id) }
            }
            listOf(song.id)
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(onDismiss = { showErrorPlaylistAddDialog = false; onDismiss() }) {
            item {
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }
            items(listOf(song)) { SongListItem(song = it) }
        }
    }

    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(items = song.artists, key = { it.id }) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(ListThumbnailSize).clip(CircleShape),
                        )
                    }
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    val bottomSheetPageState = LocalBottomSheetPageState.current

    
    if (showInfoDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showInfoDialog = false; onDismiss() },
            containerColor = Color(0xFF0D2D3A),
            icon = {
                Icon(painter = painterResource(R.drawable.info), contentDescription = null, tint = accentColor)
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false; onDismiss() }) {
                    Text(stringResource(android.R.string.ok), color = accentColor)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title)   to song.song.title,
                        stringResource(R.string.song_artists) to song.artists.joinToString { it.name },
                        stringResource(R.string.media_id)     to song.id,
                        "Itag"                                to format?.itag?.toString(),
                        stringResource(R.string.mime_type)    to format?.mimeType,
                        stringResource(R.string.codecs)       to format?.codecs,
                        stringResource(R.string.bitrate)      to format?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate)  to format?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness)     to format?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume)       to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size)    to format?.contentLength?.let {
                            Formatter.formatShortFileSize(context, it)
                        },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.55f))
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(displayText))
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                }
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    
    SongListItem(
        song = song,
        badges = {},
        trailingContent = {
            IconButton(onClick = { database.query { update(song.song.toggleLike()) } }) {
                Icon(
                    painter = painterResource(if (song.song.liked) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
    )

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

    
    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        GridMenuItem(icon = R.drawable.playlist_add, tint = { accentColor }, title = R.string.add_to_playlist) {
            showChoosePlaylistDialog = true
        }

        GridMenuItem(icon = R.drawable.share, tint = { accentColor }, title = R.string.share) {
            onDismiss()
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${song.song.title} - ${song.artists.joinToString { it.name }}")
            }
            context.startActivity(Intent.createChooser(intent, null))
        }

        if (song.artists.isNotEmpty()) {
            GridMenuItem(icon = R.drawable.artist, tint = { accentColor }, title = R.string.view_artist) {
                if (song.artists.size == 1) {
                    navController.navigate("artist/${song.artists[0].id}")
                    onDismiss()
                } else {
                    showSelectArtistDialog = true
                }
            }
        }

        if (song.song.albumId != null) {
            GridMenuItem(icon = R.drawable.album, tint = { accentColor }, title = R.string.view_album) {
                onDismiss()
                navController.navigate("album/${song.song.albumId}")
            }
        }

        GridMenuItem(icon = R.drawable.playlist_play, tint = { accentColor }, title = R.string.play_next) {
            onDismiss()
            playerConnection.playNext(song.toSafeMediaItem())
        }

        GridMenuItem(icon = R.drawable.queue_music, tint = { accentColor }, title = R.string.add_to_queue) {
            onDismiss()
            playerConnection.addToQueue(song.toSafeMediaItem())
        }

        when (download?.state) {
            Download.STATE_COMPLETED -> {
                GridMenuItem(icon = R.drawable.offline, tint = { accentColor }, title = R.string.remove_download) {
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                }
            }
            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                GridMenuItem(
                    icon = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = accentColor,
                        )
                    },
                    title = R.string.downloading,
                ) {
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                }
            }
            else -> {
                GridMenuItem(icon = R.drawable.download, tint = { accentColor }, title = R.string.download) {
                    val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                        .setCustomCacheKey(song.id)
                        .setData(song.song.title.toByteArray())
                        .build()
                    DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                }
            }
        }

        if (song.song.inLibrary == null) {
            GridMenuItem(icon = R.drawable.library_add, tint = { accentColor }, title = R.string.add_to_library) {
                database.query { update(song.song.toggleLibrary()) }
            }
        } else {
            GridMenuItem(icon = R.drawable.library_add_check, tint = { accentColor }, title = R.string.remove_from_library) {
                database.query { update(song.song.toggleLibrary()) }
            }
        }

        GridMenuItem(icon = R.drawable.edit, tint = { accentColor }, title = R.string.edit) {
            showEditDialog = true
        }

        if (event != null) {
            GridMenuItem(icon = R.drawable.delete, tint = { accentColor }, title = R.string.remove_from_history) {
                onDismiss()
                database.query { delete(event) }
            }
        }

        if (playlistSong != null) {
            GridMenuItem(icon = R.drawable.delete, tint = { accentColor }, title = R.string.remove_from_playlist) {
                database.transaction {
                    coroutineScope.launch {
                        playlistBrowseId?.let { playlistId ->
                            if (playlistSong.map.setVideoId != null) {
                                AudiusApi.removeFromPlaylist(playlistId, playlistSong.map.songId, playlistSong.map.setVideoId)
                            }
                        }
                    }
                    move(playlistSong.map.playlistId, playlistSong.map.position, Int.MAX_VALUE)
                    delete(playlistSong.map.copy(position = Int.MAX_VALUE))
                }
                onDismiss()
            }
        }

        GridMenuItem(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                )
            },
            title = R.string.refetch,
        ) {
            refetchIconDegree -= 360
            scope.launch(Dispatchers.IO) {
                AudiusApi.queue(listOf(song.id)).onSuccess { songs ->
                    val newSong = songs.firstOrNull()
                    if (newSong != null) {
                        database.transaction { update(song, newSong.toMediaMetadata()) }
                    }
                }
            }
        }

        GridMenuItem(icon = R.drawable.info, tint = { accentColor }, title = R.string.song_title) {
            showInfoDialog = true
        }
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