/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "set_video_id")
data class SetVideoIdEntity(
    @PrimaryKey(autoGenerate = false)
    val videoId: String = "",
    val setVideoId: String? = null,
)


