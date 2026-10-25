/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius.models

   
                                                                          
                                                                   
                                                                                  
   
data class AudiusArtist(
    val id: String,
    val name: String,
    val handle: String?,
    val avatarUrl: String?,
    val followerCount: Long?,
    val bio: String?,
    val profileUrl: String,
)

data class AudiusSong(
    val id: String,
    val title: String,
    val artist: AudiusArtist,
    val durationSeconds: Int?,
    val artworkUrl: String?,
    val genre: String?,
    val isStreamable: Boolean,
    val isDownloadable: Boolean,
    val url: String,
)

data class AudiusPlaylist(
    val id: String,
    val title: String,
    val owner: AudiusArtist,
    val artworkUrl: String?,
    val trackCount: Int,
    val isAlbum: Boolean,
    val description: String?,
    val url: String,
)

data class AudiusSearchResults(
    val songs: List<AudiusSong>,
    val artists: List<AudiusArtist>,
    val playlists: List<AudiusPlaylist>,
)

enum class TrendingTime(val value: String) {
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    ALL_TIME("allTime"),
}

                                                                              
data class AudiusStream(
    val url: String,
    val contentLength: Long?,
    val mimeType: String = "audio/mpeg",
    val bitrate: Int = 320_000,
    val sampleRate: Int = 44_100,
)
