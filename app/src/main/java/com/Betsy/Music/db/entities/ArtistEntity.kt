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
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "artist")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val songCount: Int = 0,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null
) {

    
    val isYouTubeArtist: Boolean
        get() = !isLocalArtist

    val isPrivatelyOwnedArtist: Boolean
        get() = id.startsWith("FEmusic_library_privately_owned_artist")

    val isLocalArtist: Boolean
        get() = id.length == 10 && id.startsWith("LA") && id.all { it.isLetter() }

    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now(),
    )

    fun toggleLike() = localToggleLike()

    companion object {
        fun generateArtistId() = "LA" + RandomStringUtils.random(8, true, false)
    }
}


