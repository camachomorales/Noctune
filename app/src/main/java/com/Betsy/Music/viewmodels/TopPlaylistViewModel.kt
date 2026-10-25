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
import com.betsy.music.constants.MyTopFilter
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TopPlaylistViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: musicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val top = savedStateHandle.get<String>("top")!!

    val topPeriod = MutableStateFlow(MyTopFilter.ALL_TIME)

    
    private val isStreamingMode =
        context.dataStore.data
            .map { it[StreamingModeKey] ?: true }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val topSongs =
        combine(topPeriod, isStreamingMode) { period, isStreaming -> period to isStreaming }
            .flatMapLatest { (period, isStreaming) ->
                
                database.mostPlayedSongs(period.toTimeMillis(), Int.MAX_VALUE)
                    .map { songs ->
                        songs
                            .filter { it.song.id.startsWith("device_") != isStreaming }
                            .take(top.toInt())
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}