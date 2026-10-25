/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.lyrics

import android.content.Context
import com.betsy.audius.compat.AudiusApi

object StreamingSubtitleLyricsProvider : LyricsProvider {
    override val name = "Streaming Subtitle"

    override fun isEnabled(context: Context) = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = AudiusApi.transcript(id)
}



