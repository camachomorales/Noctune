/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.playback.queues

import androidx.media3.common.MediaItem
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class StreamingQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        
        val iaItem = preloadItem
        if (iaItem != null && InternetArchive.isIaId(endpoint.videoId)) {
            return Queue.Status(title = null, items = listOf(iaItem.toMediaItem()), mediaItemIndex = 0)
        }
        val nextResult =
            withContext(IO) {
                AudiusApi.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return Queue.Status(
            title = nextResult.title,
            items = nextResult.items.map { it.toMediaItem() },
            mediaItemIndex = nextResult.currentIndex ?: 0,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult =
            withContext(IO) {
                AudiusApi.next(endpoint, continuation).getOrThrow()
            }
        endpoint = nextResult.endpoint
        continuation = nextResult.continuation
        return nextResult.items.map { it.toMediaItem() }
    }

    companion object {
        fun radio(song: MediaMetadata) = StreamingQueue(WatchEndpoint(song.id), song)
    }
}



