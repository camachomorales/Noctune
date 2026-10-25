/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius.compat.pages

import com.betsy.audius.compat.models.*

data class HomePage(
    val sections: List<Section>,
) {
    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
    )
}

data class MoodAndGenres(
    val title: String,
    val items: List<Item>,
) {
    data class Item(
        val title: String,
        val stripeColor: Long,
        val endpoint: BrowseEndpoint,
    )
}

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)

data class BrowseResult(
    val title: String?,
    val items: List<Item>,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
    )

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(
                items = items.mapNotNull {
                    it.copy(items = it.items.filterExplicit().ifEmpty { return@mapNotNull null })
                },
            )
        } else this
}

data class ArtistSection(
    val title: String,
    val items: List<YTItem>,
    val moreEndpoint: BrowseEndpoint?,
)

data class ArtistPage(
    val artist: ArtistItem,
    val sections: List<ArtistSection>,
    val description: String?,
)

data class ArtistItemsPage(
    val title: String,
    val items: List<YTItem>,
    val continuation: String?,
)

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
)

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
)

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

data class SearchSummary(
    val title: String,
    val items: List<YTItem>,
)

data class SearchSummaryPage(
    val summaries: List<SearchSummary>,
) {
    fun filterExplicit(enabled: Boolean) =
        if (enabled) {
            SearchSummaryPage(
                summaries.mapNotNull { s ->
                    SearchSummary(
                        title = s.title,
                        items = s.items.filterExplicit().ifEmpty { return@mapNotNull null },
                    )
                },
            )
        } else this
}

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint,
)

data class RelatedPage(
    val songs: List<SongItem>,
    val albums: List<AlbumItem>,
    val artists: List<ArtistItem>,
    val playlists: List<PlaylistItem>,
)

                                                                                            
data class LibraryPage(
    val items: List<YTItem>,
    val continuation: String?,
)

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)

data class HistoryPage(
    val sections: List<HistorySection>?,
) {
    data class HistorySection(
        val title: String,
        val songs: List<SongItem>,
    )
}
