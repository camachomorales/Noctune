/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.filterExplicit
import com.betsy.audius.compat.pages.ExplorePage
import com.betsy.music.constants.HideExplicitKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.utils.ContentLocale
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import com.betsy.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: musicDatabase,
) : ViewModel() {
    val explorePage = MutableStateFlow<ExplorePage?>(null)

    private suspend fun load() {
        AudiusApi
            .explore()
            .onSuccess { page ->
                val artists: MutableMap<Int, String> = mutableMapOf()
                val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                database.allArtistsByPlayTime().first().let { list ->
                    var favIndex = 0
                    for ((artistsIndex, artist) in list.withIndex()) {
                        artists[artistsIndex] = artist.id
                        if (artist.artist.bookmarkedAt != null) {
                            favouriteArtists[favIndex] = artist.id
                            favIndex++
                        }
                    }
                }
                explorePage.value =
                    page.copy(
                        newReleaseAlbums =
                            page.newReleaseAlbums
                                .sortedBy { album ->
                                    val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                    val firstArtistKey =
                                        artistIds.firstNotNullOfOrNull { artistId ->
                                            if (artistId in favouriteArtists.values) {
                                                favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                            } else {
                                                artists.entries.firstOrNull { it.value == artistId }?.key
                                            }
                                        } ?: Int.MAX_VALUE
                                    firstArtistKey
                                }.filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                    )
            }.onFailure {
                reportException(it)
            }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ContentLocale.flow(context).collectLatest { load() }
        }
    }
}