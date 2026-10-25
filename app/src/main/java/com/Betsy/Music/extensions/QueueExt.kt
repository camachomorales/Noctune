/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.extensions

import com.betsy.music.models.MediaMetadata
import com.betsy.music.models.PersistQueue
import com.betsy.music.models.QueueData
import com.betsy.music.models.QueueType
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.playback.queues.LocalAlbumRadio
import com.betsy.music.playback.queues.Queue
import com.betsy.music.playback.queues.StreamingAlbumRadio
import com.betsy.music.playback.queues.StreamingQueue


fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long
): PersistQueue {
    return when (this) {
        is ListQueue -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
        is StreamingQueue -> {
            
            val endpoint = "youtube_queue"
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE,
                queueData = QueueData.YouTubeData(endpoint = endpoint)
            )
        }
        is StreamingAlbumRadio -> {
            
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE_ALBUM_RADIO,
                queueData = QueueData.YouTubeAlbumRadioData(
                    playlistId = "youtube_album_radio"
                )
            )
        }
        is LocalAlbumRadio -> {
            
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.LOCAL_ALBUM_RADIO,
                queueData = QueueData.LocalAlbumRadioData(
                    albumId = "local_album_radio",
                    startIndex = 0
                )
            )
        }
        else -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
    }
}

fun PersistQueue.toQueue(): Queue {
    return when (queueType) {
        is QueueType.LIST -> ListQueue(
            title = title,
            items = items.map { it.toMediaItem() },
            startIndex = mediaItemIndex,
            position = position
        )
        is QueueType.YOUTUBE -> {
            
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position
            )
        }
        is QueueType.YOUTUBE_ALBUM_RADIO -> {
            
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position
            )
        }
        is QueueType.LOCAL_ALBUM_RADIO -> {
            
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = mediaItemIndex,
                position = position
            )
        }
    }
}

