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
data class Song
@JvmOverloads
constructor(
    @Embedded val song: SongEntity,
    @Relation(
        entity = ArtistEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy =
            Junction(
                value = SortedSongArtistMap::class,
                parentColumn = "songId",
                entityColumn = "artistId",
            ),
    )
    val artists: List<ArtistEntity>,
    @Relation(
        entity = AlbumEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy =
            Junction(
                value = SongAlbumMap::class,
                parentColumn = "songId",
                entityColumn = "albumId",
            ),
    )
    val album: AlbumEntity? = null,
) : LocalItem() {
    override val id: String
        get() = song.id
    override val title: String
        get() = song.title
    override val thumbnailUrl: String?
        get() = song.thumbnailUrl
}


