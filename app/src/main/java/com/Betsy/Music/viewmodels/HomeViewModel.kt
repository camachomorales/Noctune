/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.viewmodels

import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.audius.compat.models.YTItem
import com.betsy.audius.compat.pages.ExplorePage
import com.betsy.audius.compat.pages.HomePage
import com.betsy.audius.compat.utils.completedLibraryPage
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.db.musicDatabase
import com.betsy.music.db.entities.Album
import com.betsy.music.db.entities.Artist
import com.betsy.music.db.entities.LocalItem
import com.betsy.music.db.entities.Playlist
import com.betsy.music.db.entities.Song
import com.betsy.music.models.SimilarRecommendation
import com.betsy.music.utils.AccountManager
import com.betsy.music.utils.ContentLocale
import com.betsy.music.utils.GuestManager
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.reportException
import com.betsy.music.utils.toAlbumItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val database: musicDatabase,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    val trendingSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val localTrendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val madeForYouPlaylists = MutableStateFlow<List<PlaylistItem>>(emptyList())


    val iaAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())

    val currentUserId = MutableStateFlow("")
    val isGuest = MutableStateFlow(true)
    val guestShortId = MutableStateFlow("")



    init {
        viewModelScope.launch(Dispatchers.IO) {


            combine(
                context.dataStore.data
                    .map { it[stringPreferencesKey("active_account_id")] ?: "" }
                    .distinctUntilChanged(),
                ContentLocale.flow(context)
            ) { _, _ -> }
                .collectLatest {
                    load()
                }
        }
    }

    private suspend fun handleAuthError(throwable: Throwable) {
        if (throwable is ClientRequestException && throwable.response.status.value == 401) {
            val guestId = GuestManager.getOrCreateGuestId(context.dataStore)
            isGuest.value = true
            currentUserId.value = guestId
            guestShortId.value = GuestManager.shortId(guestId)
            Log.w("HomeViewModel", "Session expired, switched to guest mode")
        }
        reportException(throwable)
    }

    private suspend fun safeLibraryCall(
        params: String,
        onSuccess: (List<PlaylistItem>) -> Unit
    ) {
        runCatching {
            AudiusApi.library(params).completedLibraryPage()
        }.onSuccess { result ->
            result.onSuccess { page ->
                onSuccess(page.items.filterIsInstance<PlaylistItem>())
            }.onFailure { e ->
                handleAuthError(e)
            }
        }.onFailure { e ->
            handleAuthError(e)
        }
    }

    private suspend fun load() {
        if (_isLoading.value) return
        _isLoading.value = true

        try {

            val guestId = GuestManager.getOrCreateGuestId(context.dataStore)
            val activeId = AccountManager.getActiveAccountId(context.dataStore)
            val loggedIn = !GuestManager.isGuest(activeId)

            if (loggedIn) {
                isGuest.value = false
                currentUserId.value = activeId
                guestShortId.value = ""
            } else {
                isGuest.value = true
                currentUserId.value = guestId
                guestShortId.value = GuestManager.shortId(guestId)
            }


            quickPicks.value = database.quickPicks().first().shuffled().take(20)
            forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20)

            val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2

            val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
                .first().shuffled().take(10)
            val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
                .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
            val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
                .first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
                .shuffled().take(5)
            keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

            allLocalItems.value =
                (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
                    .filter { it is Song || it is Album }

            localTrendingSongs.value = database.mostPlayedSongs(fromTimeStamp, limit = 10, offset = 0).first()


            viewModelScope.launch(Dispatchers.IO) {
                iaAlbums.value = InternetArchive.client
                    .search(IA_HOME_QUERY, page = 1, rows = IA_HOME_ROWS)
                    .getOrNull()?.items.orEmpty()
                    .map { it.toAlbumItem() }
            }


            AudiusApi.playlist(AudiusApi.TRENDING_ID).onSuccess { page ->
                trendingSongs.value = page.songs.take(20)
            }.onFailure { reportException(it) }


            AudiusApi.home().onSuccess { page ->
                homePage.value = page
                if (trendingSongs.value.isEmpty()) {
                    trendingSongs.value = page.sections
                        .flatMap { it.items }
                        .filterIsInstance<SongItem>()
                        .take(20)
                }
            }.onFailure { reportException(it) }


            madeForYouPlaylists.value = emptyList()
            accountPlaylists.value = emptyList()
            similarRecommendations.value = emptyList()


            AudiusApi.explore().onSuccess { page ->
                val artists: Set<String>
                val favouriteArtists: Set<String>
                database.artistsBookmarkedByCreateDateAsc().first().let { list ->
                    artists = list.map(Artist::id).toHashSet()
                    favouriteArtists = list.filter { it.artist.bookmarkedAt != null }.map { it.id }.toHashSet()
                }
                explorePage.value = page.copy(
                    newReleaseAlbums = page.newReleaseAlbums.sortedBy { album ->
                        if (album.artists.orEmpty().any { it.id in favouriteArtists }) 0
                        else if (album.artists.orEmpty().any { it.id in artists }) 1
                        else 2
                    }
                )
            }.onFailure { reportException(it) }

            allYtItems.value =
                similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                        homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                        explorePage.value?.newReleaseAlbums.orEmpty()

        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading home data", e)
            reportException(e)
        } finally {
            _isLoading.value = false
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            load()
            _isRefreshing.value = false
        }
    }
}


private const val IA_HOME_QUERY = "music"
private const val IA_HOME_ROWS = 15