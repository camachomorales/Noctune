/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius

import com.betsy.audius.models.*

private const val SITE = "https://audius.co"

internal fun UserDto.toArtist() = AudiusArtist(
    id = id,
    name = name?.takeIf { it.isNotBlank() } ?: handle ?: "Artista desconocido",
    handle = handle,
    avatarUrl = profilePicture?.best(),
    followerCount = followerCount,
    bio = bio,
    profileUrl = if (handle != null) "$SITE/$handle" else SITE,
)

private val unknownArtist = AudiusArtist("", "Artista desconocido", null, null, null, null, SITE)

internal fun TrackDto.toSong() = AudiusSong(
    id = id,
    title = title ?: "Sin título",
    artist = user?.toArtist() ?: unknownArtist,
    durationSeconds = duration,
    artworkUrl = artwork?.best(),
    genre = genre,
    isStreamable = isStreamable ?: true,
    isDownloadable = isDownloadable ?: false,
    url = if (permalink != null) "$SITE$permalink" else SITE,
)

internal fun PlaylistDto.toPlaylist() = AudiusPlaylist(
    id = id,
    title = name ?: "Sin título",
    owner = user?.toArtist() ?: unknownArtist,
    artworkUrl = artwork?.best(),
    trackCount = trackCount ?: 0,
    isAlbum = isAlbum ?: false,
    description = description,
    url = if (permalink != null) "$SITE$permalink" else SITE,
)
