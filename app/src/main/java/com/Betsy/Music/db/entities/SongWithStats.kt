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







@Immutable
data class SongWithStats(
    val id: String,
    val title: String,
    
    val thumbnailUrl: String?,
    val songCountListened: Int,
    val timeListened: Long?,
)
