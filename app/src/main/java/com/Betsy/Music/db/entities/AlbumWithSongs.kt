/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class AlbumWithSongs(
    @Embedded
    val album: AlbumEntity,
    @Relation(
        entity = ArtistEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy =
            Junction(
                value = AlbumArtistMap::class,
                parentColumn = "albumId",
                entityColumn = "artistId",
            ),
    )
    val artists: List<ArtistEntity>,
    @Relation(
        entity = SongEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy =
            Junction(
                value = SortedSongAlbumMap::class,
                parentColumn = "albumId",
                entityColumn = "songId",
            ),
    )
    val songs: List<Song>,
)


