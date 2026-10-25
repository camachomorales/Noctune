/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.lyrics

import android.content.Context
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.WatchEndpoint

object StreamingLyricsProvider : LyricsProvider {
    override val name = "Streaming"

    override fun isEnabled(context: Context) = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> =
        runCatching {
            val nextResult = AudiusApi.next(WatchEndpoint(videoId = id)).getOrThrow()
            AudiusApi
                .lyrics(
                    endpoint = nextResult.lyricsEndpoint
                        ?: throw IllegalStateException("Lyrics endpoint not found"),
                ).getOrThrow() ?: throw IllegalStateException("Lyrics unavailable")
        }
}



