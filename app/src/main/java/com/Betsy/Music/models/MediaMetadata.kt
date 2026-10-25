/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.models

import androidx.compose.runtime.Immutable
import com.betsy.audius.compat.models.SongItem
import com.betsy.music.db.entities.Song
import com.betsy.music.db.entities.SongEntity
import com.betsy.music.ui.utils.resize
import java.io.Serializable

@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val setVideoId: String? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
) : Serializable {
    data class Artist(
        val id: String?,
        val name: String,
    ) : Serializable

    data class Album(
        val id: String,
        val title: String,
    ) : Serializable

    fun toSongEntity() =
        SongEntity(
            id = id,
            title = title,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            albumId = album?.id,
            albumName = album?.title,
        )
}

fun Song.toMediaMetadata() =
    MediaMetadata(
        id = song.id,
        title = song.title,
        artists =
            artists.map {
                MediaMetadata.Artist(
                    id = it.id,
                    name = it.name,
                )
            },
        duration = song.duration,
        thumbnailUrl = song.thumbnailUrl,
        album =
            album?.let {
                MediaMetadata.Album(
                    id = it.id,
                    title = it.title,
                )
            } ?: song.albumId?.let { albumId ->
                
                
                song.albumName?.takeIf { it.isNotBlank() }?.let { albumName ->
                    MediaMetadata.Album(
                        id = albumId,
                        title = albumName,
                    )
                }
            },
    )

fun SongItem.toMediaMetadata() =
    MediaMetadata(
        id = id,
        title = title,
        artists =
            artists.map {
                MediaMetadata.Artist(
                    id = it.id,
                    name = it.name,
                )
            },
        duration = duration ?: -1,
        thumbnailUrl = thumbnail.resize(544, 544),
        album =
            album?.let {
                
                it.name.takeIf { name -> name.isNotBlank() }?.let { albumName ->
                    MediaMetadata.Album(
                        id = it.id,
                        title = albumName,
                    )
                }
            },
        explicit = explicit,
        setVideoId = setVideoId
    )


