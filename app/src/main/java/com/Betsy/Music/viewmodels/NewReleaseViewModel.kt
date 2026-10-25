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
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.filterExplicit
import com.betsy.music.constants.HideExplicitKey
import com.betsy.music.constants.LastNewReleaseCheckKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.utils.ContentLocale
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import com.betsy.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewReleaseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: musicDatabase,
) : ViewModel() {
    private val _newReleaseAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    private val _hasNewReleases = MutableStateFlow(false)
    val hasNewReleases = _hasNewReleases.asStateFlow()

    init {
        viewModelScope.launch {
            ContentLocale.flow(context).collectLatest {
                AudiusApi
                    .newReleaseAlbums()
                    .onSuccess { albums ->
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

                        val sortedAlbums = albums
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
                            }.filterExplicit(context.dataStore.get(HideExplicitKey, false))

                        _newReleaseAlbums.value = sortedAlbums

                        
                        checkForNewReleases()

                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }



    private suspend fun checkForNewReleases() {
        try {
            val lastCheckTime = context.dataStore.get(LastNewReleaseCheckKey, 0L)
            val currentTime = System.currentTimeMillis()

            
            if (lastCheckTime == 0L) {
                context.dataStore.updateData { it.toMutablePreferences().apply {
                    set(LastNewReleaseCheckKey, currentTime)
                }}
                _hasNewReleases.value = false
                return
            }

            
            val hasNewReleases = _newReleaseAlbums.value.isNotEmpty() &&
                    (currentTime - lastCheckTime) > (24 * 60 * 60 * 1000) 

            _hasNewReleases.value = hasNewReleases

        } catch (e: Exception) {
            reportException(e)
            _hasNewReleases.value = false
        }
    }

    fun markNewReleasesAsSeen() {
        viewModelScope.launch {
            try {
                context.dataStore.updateData { it.toMutablePreferences().apply {
                    set(LastNewReleaseCheckKey, System.currentTimeMillis())
                }}
                _hasNewReleases.value = false
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }
}