/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius.compat.models

import kotlinx.serialization.Serializable

   
                                                                                                 
                                                                 
   
sealed class YTItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnail: String
    abstract val explicit: Boolean
    abstract val shareLink: String
}

data class Artist(
    val name: String,
    val id: String?,
)

data class Album(
    val name: String,
    val id: String,
)

private const val SITE = "https://audius.co"

data class SongItem(
    override val id: String,
    override val title: String,
    val artists: List<Artist>,
    val album: Album? = null,
    val duration: Int? = null,
    val setVideoId: String? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val endpoint: WatchEndpoint? = null,
                                                                             
    val webUrl: String? = null,
    val isStreamable: Boolean = true,
    val isDownloadable: Boolean = false,
) : YTItem() {
    override val shareLink: String
        get() = webUrl ?: SITE
}

data class AlbumItem(
    val browseId: String,
    val playlistId: String,
    override val id: String = browseId,
    override val title: String,
    val artists: List<Artist>?,
    val year: Int? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val webUrl: String? = null,
) : YTItem() {
    override val shareLink: String
        get() = webUrl ?: SITE
}

data class PlaylistItem(
    override val id: String,
    override val title: String,
    var author: Artist?,
    val songCountText: String?,
    override val thumbnail: String,
    val playEndpoint: WatchEndpoint?,
    val shuffleEndpoint: WatchEndpoint?,
    val radioEndpoint: WatchEndpoint?,
    val isEditable: Boolean = false,
    val webUrl: String? = null,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = webUrl ?: SITE
}

data class ArtistItem(
    override val id: String,
    override val title: String,
    override val thumbnail: String,
    val channelId: String? = null,
    val shuffleEndpoint: WatchEndpoint?,
    val radioEndpoint: WatchEndpoint?,
    val subscriberCountText: String? = null,
    val webUrl: String? = null,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = webUrl ?: SITE
}

fun <T : YTItem> List<T>.filterExplicit(enabled: Boolean = true) =
    if (enabled) filter { !it.explicit } else this

                                                            
@Serializable
data class WatchEndpoint(
    val videoId: String? = null,
    val playlistId: String? = null,
    val playlistSetVideoId: String? = null,
    val params: String? = null,
    val index: Int? = null,
)

   
                                         
                                                        
                                                                              
   
@Serializable
data class BrowseEndpoint(
    val browseId: String,
    val params: String? = null,
    val pageType: String? = null,
) {
    val isArtistEndpoint: Boolean get() = pageType == PAGE_ARTIST
    val isAlbumEndpoint: Boolean get() = pageType == PAGE_ALBUM
    val isPlaylistEndpoint: Boolean get() = pageType == PAGE_PLAYLIST

    companion object {
        const val PAGE_ARTIST = "artist"
        const val PAGE_ALBUM = "album"
        const val PAGE_PLAYLIST = "playlist"
    }
}

                                                                                          
@Serializable
data class StreamingLocale(
    val gl: String,
    val hl: String,
)

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<YTItem>,
)

data class AccountInfo(
    val name: String,
    val email: String?,
    val channelHandle: String?,
)
