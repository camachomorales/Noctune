/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.SongItem
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalDownloadUtil
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.ListItemHeight
import com.betsy.music.constants.ListThumbnailSize
import com.betsy.music.constants.ThumbnailCornerRadius
import com.betsy.music.db.entities.SongEntity
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.models.MediaMetadata
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.ExoDownloadService
import com.betsy.music.ui.component.GridMenu
import com.betsy.music.ui.component.GridMenuItem
import com.betsy.music.ui.component.ListDialog
import com.betsy.music.ui.component.ListItem
import com.betsy.music.utils.joinByBullet
import com.betsy.music.utils.makeTimeString
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun StreamingSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val artists = remember {
        song.artists.mapNotNull {
            it.id?.let { artistId -> MediaMetadata.Artist(id = artistId, name = it.name) }
        }
    }

    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction { insert(song.toMediaMetadata()) }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { AudiusApi.addToPlaylist(it, song.id) }
            }
            listOf(song.id)
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    
    ListItem(
        title = song.title,
        subtitle = joinByBullet(
            song.artists.joinToString { it.name },
            song.duration?.let { makeTimeString(it * 1000L) },
        ),
        thumbnailContent = {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    database.transaction {
                        librarySong.let { librarySong ->
                            if (librarySong == null) {
                                insert(song.toMediaMetadata(), SongEntity::toggleLike)
                            } else {
                                update(librarySong.song.toggleLike())
                            }
                        }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(
                        if (librarySong?.song?.liked == true) R.drawable.favorite
                        else R.drawable.favorite_border
                    ),
                    tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error
                    else LocalContentColor.current,
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
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, song.shareLink)
            }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }

        if (artists.isNotEmpty()) {
            GridMenuItem(icon = R.drawable.artist, tint = { accentColor }, title = R.string.view_artist) {
                if (artists.size == 1) {
                    navController.navigate("artist/${artists[0].id}")
                    onDismiss()
                } else {
                    showSelectArtistDialog = true
                }
            }
        }

        song.album?.let { album ->
            GridMenuItem(icon = R.drawable.album, tint = { accentColor }, title = R.string.view_album) {
                navController.navigate("album/${album.id}")
                onDismiss()
            }
        }

        GridMenuItem(icon = R.drawable.playlist_play, tint = { accentColor }, title = R.string.play_next) {
            playerConnection.playNext(song.toMediaItem())
            onDismiss()
        }

        GridMenuItem(icon = R.drawable.queue_music, tint = { accentColor }, title = R.string.add_to_queue) {
            playerConnection.addToQueue(song.toMediaItem())
            onDismiss()
        }

        when (download?.state) {
            Download.STATE_COMPLETED -> {
                GridMenuItem(icon = R.drawable.offline, tint = { accentColor }, title = R.string.remove_download) {
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                }
            }
            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                GridMenuItem(
                    icon = { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = accentColor) },
                    title = R.string.downloading,
                ) {
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                }
            }
            else -> {
                GridMenuItem(icon = R.drawable.download, tint = { accentColor }, title = R.string.download) {
                    database.transaction { insert(song.toMediaMetadata()) }
                    val downloadRequest = DownloadRequest.Builder(song.id, song.id.toUri())
                        .setCustomCacheKey(song.id)
                        .setData(song.title.toByteArray())
                        .build()
                    DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                }
            }
        }

        if (librarySong?.song?.inLibrary != null) {
            GridMenuItem(icon = R.drawable.library_add_check, tint = { accentColor }, title = R.string.remove_from_library) {
                database.query { inLibrary(song.id, null) }
            }
        } else {
            GridMenuItem(icon = R.drawable.library_add, tint = { accentColor }, title = R.string.add_to_library) {
                database.transaction {
                    insert(song.toMediaMetadata())
                    inLibrary(song.id, LocalDateTime.now())
                }
            }
        }
    }
} 