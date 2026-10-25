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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.betsy.music.constants.SongSortDescendingKey
import com.betsy.music.constants.SongSortType
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.Song
import com.betsy.music.constants.SongSortTypeKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.extensions.reversed
import com.betsy.music.extensions.toEnum
import com.betsy.music.playback.DownloadUtil
import com.betsy.music.utils.SyncUtils
import com.betsy.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
    downloadUtil: DownloadUtil,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedSongs =
        context.dataStore.data
            .map {
                Triple(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                    it[SongSortDescendingKey] ?: true,
                    it[StreamingModeKey] ?: true,
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { (sortType, descending, isStreaming) ->
                val songsFlow: Flow<List<Song>> = when (playlist) {
                    "liked" -> database.likedSongs(sortType, descending)
                    "downloaded" -> downloadUtil.downloads.flatMapLatest { downloads ->
                        database.allSongs()
                            .flowOn(Dispatchers.IO)
                            .map { songs ->
                                songs.filter {
                                    downloads[it.id]?.state == Download.STATE_COMPLETED
                                }
                            }
                            .map { songs ->
                                when (sortType) {
                                    SongSortType.CREATE_DATE -> songs.sortedBy {
                                        downloads[it.id]?.updateTimeMs ?: 0L
                                    }

                                    SongSortType.NAME -> songs.sortedBy { it.song.title }
                                    SongSortType.ARTIST -> songs.sortedBy { song ->
                                        song.artists.joinToString(separator = "") { it.name }
                                    }

                                    SongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                                }.reversed(descending)
                            }
                    }

                    else -> MutableStateFlow(emptyList())
                }
                
                songsFlow.map { songs -> songs.filter { it.song.id.startsWith("device_") != isStreaming } }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }
}