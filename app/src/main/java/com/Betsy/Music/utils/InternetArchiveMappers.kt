/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.utils

import android.net.Uri
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.Artist
import com.betsy.internetarchive.IaItem
import com.betsy.internetarchive.IaTrack
import com.betsy.music.models.MediaMetadata

                                                                                               
fun IaTrack.toMediaMetadata() =
    MediaMetadata(
        id = id,
        title = title,
        artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
        duration = durationSeconds ?: -1,
        thumbnailUrl = thumbnailUrl,
        album = null,
    )

                                                                                                     
const val IA_ALBUM_PREFIX = "iaitem:"

fun isIaAlbumId(id: String) = id.startsWith(IA_ALBUM_PREFIX)

fun iaIdentifierOf(albumId: String) = albumId.removePrefix(IA_ALBUM_PREFIX)

   
                                                                                   
                                                                                       
                                                                                              
   
const val AUDIUS_PLAYLIST_ALBUM_PREFIX = "aupl:"

fun isAudiusPlaylistAlbumId(id: String) = id.startsWith(AUDIUS_PLAYLIST_ALBUM_PREFIX)

fun audiusPlaylistIdOf(albumId: String) = albumId.removePrefix(AUDIUS_PLAYLIST_ALBUM_PREFIX)

                                                                                                   
fun isProxyAlbumId(id: String) = isIaAlbumId(id) || isAudiusPlaylistAlbumId(id)

   
                                                                                                
                                                                                                  
   
const val IA_PLAYLIST_ID_PREFIX = "LPIA_"

fun iaPlaylistIdOf(identifier: String) = IA_PLAYLIST_ID_PREFIX + identifier

                                                                           
fun albumRoute(albumId: String): String =
    when {
        isIaAlbumId(albumId) -> "ia_item/${Uri.encode(iaIdentifierOf(albumId))}"
        isAudiusPlaylistAlbumId(albumId) -> "online_playlist/${audiusPlaylistIdOf(albumId)}"
        else -> "album/$albumId"
    }

   
                                                                                         
                                                                                                      
   
fun IaItem.toAlbumItem() =
    AlbumItem(
        browseId = IA_ALBUM_PREFIX + identifier,
        playlistId = "",
        title = title,
        artists = listOf(
            Artist(
                name = listOfNotNull(creator?.takeIf { it.isNotBlank() }, "Internet Archive").joinToString(" • "),
                id = null,
            ),
        ),
        thumbnail = thumbnailUrl,
        webUrl = detailsUrl,
    )