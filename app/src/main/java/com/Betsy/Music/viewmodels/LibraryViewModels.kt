/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.betsy.music.viewmodels

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.Artist as InntertubeArtist
import com.betsy.audius.compat.models.Album as InnertubeAlbum
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.constants.AlbumFilter
import com.betsy.music.constants.AlbumFilterKey
import com.betsy.music.constants.AlbumSortDescendingKey
import com.betsy.music.constants.AlbumSortType
import com.betsy.music.constants.AlbumSortTypeKey
import com.betsy.music.constants.ArtistFilter
import com.betsy.music.constants.ArtistFilterKey
import com.betsy.music.constants.ArtistSongSortDescendingKey
import com.betsy.music.constants.ArtistSongSortType
import com.betsy.music.constants.ArtistSongSortTypeKey
import com.betsy.music.constants.ArtistSortDescendingKey
import com.betsy.music.constants.ArtistSortType
import com.betsy.music.constants.ArtistSortTypeKey
import com.betsy.music.constants.GuestUserIdKey
import com.betsy.music.constants.LibraryFilter
import com.betsy.music.constants.PlaylistSortDescendingKey
import com.betsy.music.constants.PlaylistSortType
import com.betsy.music.constants.PlaylistSortTypeKey
import com.betsy.music.constants.SongFilter
import com.betsy.music.constants.SongFilterKey
import com.betsy.music.constants.SongSortDescendingKey
import com.betsy.music.constants.SongSortType
import com.betsy.music.constants.SongSortTypeKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.constants.TopSize
import com.betsy.music.db.musicDatabase
import com.betsy.music.db.entities.Song
import com.betsy.music.extensions.reversed
import com.betsy.music.extensions.toEnum
import com.betsy.music.playback.DownloadUtil
import com.betsy.music.utils.SyncUtils
import com.betsy.music.utils.isIaAlbumId
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import androidx.datastore.preferences.core.stringPreferencesKey


private val activeAccountIdKey = stringPreferencesKey("active_account_id")

@HiltViewModel
class LibrarySongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allSongs =
        context.dataStore.data
            .map {
                Triple(
                    it[SongFilterKey].toEnum(SongFilter.LIKED),
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                    (it[SongSortDescendingKey] ?: true),
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    SongFilter.LIBRARY -> database.songs(sortType, descending)
                    SongFilter.LIKED -> database.likedSongs(sortType, descending)
                    SongFilter.PLAYLIST -> database.songsInUserPlaylists()
                    SongFilter.DOWNLOADED ->
                        downloadUtil.downloads.flatMapLatest { downloads ->
                            database
                                .allSongs()
                                .flowOn(Dispatchers.IO)
                                .map { songs ->
                                    songs.filter {
                                        downloads[it.id]?.state == Download.STATE_COMPLETED
                                    }
                                }.map { songs ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> songs.sortedBy {
                                            downloads[it.id]?.updateTimeMs ?: 0L
                                        }
                                        SongSortType.NAME -> songs.sortedBy { it.song.title }
                                        SongSortType.ARTIST -> {
                                            val collator = Collator.getInstance(Locale.getDefault())
                                            collator.strength = Collator.PRIMARY
                                            songs
                                                .sortedWith(
                                                    compareBy(collator) { song ->
                                                        song.artists.joinToString("") { it.name }
                                                    },
                                                ).groupBy { it.album?.title }
                                                .flatMap { (_, songsByAlbum) ->
                                                    songsByAlbum.sortedBy { album ->
                                                        album.artists.joinToString("") { it.name }
                                                    }
                                                }
                                        }
                                        SongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                                    }.reversed(descending)
                                }
                        }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncLibrarySongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLibrarySongs() }
    }
}

@HiltViewModel
class LibraryArtistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allArtists =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistFilterKey].toEnum(ArtistFilter.LIKED),
                    it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                    it[ArtistSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                    ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                    ArtistFilter.PLAYLIST -> database.artistsInUserPlaylists()
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncArtistsSubscriptions() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        AudiusApi.artist(artist.id).onSuccess { artistPage ->
                            database.query { update(artist, artistPage) }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryAlbumsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allAlbums =
        context.dataStore.data
            .map {
                Triple(
                    it[AlbumFilterKey].toEnum(AlbumFilter.LIKED),
                    it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                    it[AlbumSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    AlbumFilter.LIBRARY -> database.albums(sortType, descending)
                    AlbumFilter.LIKED -> database.albumsLiked(sortType, descending)
                    AlbumFilter.PLAYLIST -> database.albumsInUserPlaylists()
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedAlbums() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allAlbums.collect { albums ->
                albums
                    .filter { it.album.songCount == 0 && !isIaAlbumId(it.id) }
                    .forEach { album ->
                        AudiusApi
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query { update(album.album, albumPage, album.artists) }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query { delete(album.album) }
                                }
                            }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryPlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {

    val allPlaylists =
        combine(
            
            context.dataStore.data.map {
                it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to
                        (it[PlaylistSortDescendingKey] ?: true)
            }.distinctUntilChanged(),
            
            context.dataStore.data.map {
                it[StreamingModeKey] ?: true
            }.distinctUntilChanged(),
            
            
            
            
            
            
            context.dataStore.data.map {
                val activeId = it[activeAccountIdKey] ?: ""
                val guestId  = it[GuestUserIdKey] ?: "guest_fallback"
                activeId.ifBlank { guestId }
            }.distinctUntilChanged()
        ) { (sortType, descending), isStreaming, activeOwnerId ->
            Triple(sortType, descending, Pair(activeOwnerId, !isStreaming))
        }
            .flatMapLatest { (sortType, descending, ownerMode) ->
                val (ownerId, isLocalMode) = ownerMode
                database.playlists(sortType, descending, ownerId, isLocalMode)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncSavedPlaylists() }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
}

@HiltViewModel
class ArtistSongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs =
        context.dataStore.data
            .map {
                it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to
                        (it[ArtistSongSortDescendingKey] ?: true)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.artistSongs(artistId, sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
) : ViewModel() {
    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()

    var artists = database
        .artistsBookmarked(ArtistSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var albums = database.albumsLiked(AlbumSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    
    var playlists =
        combine(
            context.dataStore.data.map {
                it[StreamingModeKey] ?: true
            }.distinctUntilChanged(),
            context.dataStore.data.map {
                val activeId = it[activeAccountIdKey] ?: ""
                val guestId  = it[GuestUserIdKey] ?: "guest_fallback"
                activeId.ifBlank { guestId }
            }.distinctUntilChanged()
        ) { isStreaming, activeOwnerId ->
            Pair(activeOwnerId, !isStreaming)
        }
            .flatMapLatest { (ownerId, isLocalMode) ->
                database.playlists(PlaylistSortType.CREATE_DATE, true, ownerId, isLocalMode)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albums ->
                albums
                    .filter { it.album.songCount == 0 && !isIaAlbumId(it.id) }
                    .forEach { album ->
                        AudiusApi
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query { update(album.album, albumPage, album.artists) }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query { delete(album.album) }
                                }
                            }
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                                Duration.between(it.lastUpdateTime, LocalDateTime.now()) > Duration.ofDays(10)
                    }.forEach { artist ->
                        AudiusApi.artist(artist.id).onSuccess { artistPage ->
                            database.query { update(artist, artistPage) }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val database: musicDatabase,
) : ViewModel() {

    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen

    val recentLocalSongs: StateFlow<List<Song>> =
        database.quickPicks()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _recentYtSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val recentYtSongs: StateFlow<List<SongItem>> = _recentYtSongs

    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.quickPicks().collect { songs ->
                _recentYtSongs.value = songs
                    .filter { song -> !song.song.id.startsWith("LA") }
                    .map { song ->
                        SongItem(
                            id       = song.song.id,
                            title    = song.song.title,
                            artists  = song.artists.map { artist ->
                                InntertubeArtist(name = artist.name, id = artist.id)
                            },
                            album    = song.album?.let { album ->
                                InnertubeAlbum(name = album.title, id = album.id)
                            },
                            duration  = song.song.duration,
                            thumbnail = song.song.thumbnailUrl ?: "",
                            explicit  = false,
                            endpoint  = WatchEndpoint(videoId = song.song.id),
                        )
                    }
            }
        }
    }
}