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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betsy.music.constants.HistorySource
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private val activeAccountIdKey = stringPreferencesKey("active_account_id")
private const val LOCAL_PREFIX = "device_"

@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: musicDatabase,
) : ViewModel() {

    private val today      = LocalDate.now()
    private val thisMonday = today.with(DayOfWeek.MONDAY)
    private val lastMonday = thisMonday.minusDays(7)

    
    val isLoggedIn = context.dataStore.data
        .map { prefs ->
            val activeId = prefs[activeAccountIdKey] ?: ""
            activeId.isNotBlank() && !activeId.startsWith("guest_")
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    
    private val isStreamingMode = context.dataStore.data
        .map { prefs -> prefs[StreamingModeKey] ?: true }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    
    val historySource = MutableStateFlow(HistorySource.LOCAL)

    
    val events = database
        .events()
        .map { events ->
            agrupar(events.filter { it.song.id.startsWith(LOCAL_PREFIX) })
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    
    
    val remoteEvents = database
        .events()
        .map { events ->
            agrupar(events.filter { !it.song.id.startsWith(LOCAL_PREFIX) })
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        
        
        viewModelScope.launch {
            isStreamingMode.collect { streaming ->
                historySource.value = if (streaming) HistorySource.REMOTE else HistorySource.LOCAL
            }
        }
    }

    
    private fun agrupar(
        events: List<com.betsy.music.db.entities.EventWithSong>
    ) = events
        .groupBy {
            val date    = it.event.timestamp.toLocalDate()
            val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
            when {
                daysAgo == 0       -> DateAgo.Today
                daysAgo == 1       -> DateAgo.Yesterday
                date >= thisMonday -> DateAgo.ThisWeek
                date >= lastMonday -> DateAgo.LastWeek
                else               -> DateAgo.Other(date.withDayOfMonth(1))
            }
        }
        .toSortedMap(
            compareBy { dateAgo ->
                when (dateAgo) {
                    DateAgo.Today     -> 0L
                    DateAgo.Yesterday -> 1L
                    DateAgo.ThisWeek  -> 2L
                    DateAgo.LastWeek  -> 3L
                    is DateAgo.Other  -> ChronoUnit.DAYS.between(dateAgo.date, today)
                }
            }
        )
        .mapValues { (_, list) -> list.distinctBy { it.song.id } }
}

sealed class DateAgo {
    data object Today     : DateAgo()
    data object Yesterday : DateAgo()
    data object ThisWeek  : DateAgo()
    data object LastWeek  : DateAgo()

    class Other(val date: LocalDate) : DateAgo() {
        override fun equals(other: Any?) = other is Other && date == other.date
        override fun hashCode()          = date.hashCode()
    }
}