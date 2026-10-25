/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
internal data class AudiusResponse<T>(val data: T? = null)

@Serializable
internal data class ArtworkDto(
    @SerialName("150x150") val small: String? = null,
    @SerialName("480x480") val medium: String? = null,
    @SerialName("1000x1000") val large: String? = null,
) {
    fun best(): String? = large ?: medium ?: small
}

@Serializable
internal data class UserDto(
    val id: String,
    val handle: String? = null,
    val name: String? = null,
    val bio: String? = null,
    @SerialName("profile_picture") val profilePicture: ArtworkDto? = null,
    @SerialName("follower_count") val followerCount: Long? = null,
)

@Serializable
internal data class TrackDto(
    val id: String,
    val title: String? = null,
    val duration: Int? = null,
    val genre: String? = null,
    val permalink: String? = null,
    val artwork: ArtworkDto? = null,
    val user: UserDto? = null,
    @SerialName("is_streamable") val isStreamable: Boolean? = null,
    @SerialName("is_downloadable") val isDownloadable: Boolean? = null,
)

@Serializable
internal data class PlaylistDto(
    val id: String,
    @SerialName("playlist_name") val name: String? = null,
    val description: String? = null,
    val permalink: String? = null,
    val artwork: ArtworkDto? = null,
    val user: UserDto? = null,
    @SerialName("track_count") val trackCount: Int? = null,
    @SerialName("is_album") val isAlbum: Boolean? = null,
)
