/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil.compose.AsyncImage
import com.betsy.internetarchive.IaTrack
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalDownloadUtil
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.ListThumbnailSize
import com.betsy.music.constants.ThumbnailCornerRadius
import com.betsy.music.db.entities.SongEntity
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.playback.ExoDownloadService
import com.betsy.music.ui.component.GridMenu
import com.betsy.music.ui.component.GridMenuItem
import com.betsy.music.ui.component.ListItem
import com.betsy.music.utils.joinByBullet
import com.betsy.music.utils.makeTimeString
import com.betsy.music.utils.rememberPreference
import com.betsy.music.utils.toMediaMetadata
import java.time.LocalDateTime

   
                                                                                                
                                                                      
                                                                                   
   
@SuppressLint("MutableCollectionMutableState")
@Composable
fun IaTrackMenu(
    track: IaTrack,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val metadata = remember(track.id) { track.toMediaMetadata() }
    val librarySong by database.song(track.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(track.id).collectAsState(initial = null)

    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            
            database.transaction { insert(metadata) }
            listOf(track.id)
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    
    ListItem(
        title = track.title,
        subtitle = joinByBullet(
            track.artist,
            track.durationSeconds?.let { makeTimeString(it * 1000L) },
        ),
        thumbnailContent = {
            AsyncImage(
                model = track.thumbnailUrl,
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
                                insert(metadata, SongEntity::toggleLike)
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
                putExtra(Intent.EXTRA_TEXT, track.detailsUrl)
            }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }

        GridMenuItem(icon = R.drawable.playlist_play, tint = { accentColor }, title = R.string.play_next) {
            playerConnection.playNext(metadata.toMediaItem())
            onDismiss()
        }

        GridMenuItem(icon = R.drawable.queue_music, tint = { accentColor }, title = R.string.add_to_queue) {
            playerConnection.addToQueue(metadata.toMediaItem())
            onDismiss()
        }

        when (download?.state) {
            Download.STATE_COMPLETED -> {
                GridMenuItem(icon = R.drawable.offline, tint = { accentColor }, title = R.string.remove_download) {
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, track.id, false)
                }
            }
            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                GridMenuItem(
                    icon = { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = accentColor) },
                    title = R.string.downloading,
                ) {
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, track.id, false)
                }
            }
            else -> {
                GridMenuItem(icon = R.drawable.download, tint = { accentColor }, title = R.string.download) {
                    database.transaction { insert(metadata) }
                    val downloadRequest = DownloadRequest.Builder(track.id, track.id.toUri())
                        .setCustomCacheKey(track.id)
                        .setData(track.title.toByteArray())
                        .build()
                    DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                }
            }
        }

        if (librarySong?.song?.inLibrary != null) {
            GridMenuItem(icon = R.drawable.library_add_check, tint = { accentColor }, title = R.string.remove_from_library) {
                database.query { inLibrary(track.id, null) }
            }
        } else {
            GridMenuItem(icon = R.drawable.library_add, tint = { accentColor }, title = R.string.add_to_library) {
                database.transaction {
                    insert(metadata)
                    inLibrary(track.id, LocalDateTime.now())
                }
            }
        }
    }
}
