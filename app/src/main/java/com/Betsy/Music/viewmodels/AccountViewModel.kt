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
import com.betsy.music.constants.GuestUserIdKey
import com.betsy.music.constants.PlaylistSortType
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.music.db.musicDatabase
import com.betsy.music.utils.GuestManager
import com.betsy.music.utils.LocalAccountManager
import com.betsy.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val activeAccountIdKey = stringPreferencesKey("active_account_id")

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: musicDatabase,
) : ViewModel() {

    
    
    
    private val isStreamingMode = context.dataStore.data
        .map { it[StreamingModeKey] ?: true }
        .distinctUntilChanged()

    private fun matchesMode(songId: String, streaming: Boolean): Boolean =
        songId.startsWith("device_") != streaming

    private val modeEvents = combine(isStreamingMode, database.events()) { streaming, events ->
        events.filter { matchesMode(it.song.id, streaming) }
    }

    val songsPlayed: StateFlow<Int> = modeEvents
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val listenedMillis: StateFlow<Long> = modeEvents
        .map { list -> list.sumOf { it.event.playTime } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val favoritesCount: StateFlow<Int> =
        combine(isStreamingMode, database.allSongs()) { streaming, songs ->
            songs.count { it.song.liked && matchesMode(it.id, streaming) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val playlistsCount: StateFlow<Int> =
        combine(
            context.dataStore.data.map { it[StreamingModeKey] ?: true }.distinctUntilChanged(),
            context.dataStore.data.map {
                val activeId = it[activeAccountIdKey] ?: ""
                val guestId = it[GuestUserIdKey] ?: "guest_fallback"
                activeId.ifBlank { guestId }
            }.distinctUntilChanged(),
        ) { isStreaming, ownerId -> Pair(ownerId, !isStreaming) }
            .flatMapLatest { (ownerId, isLocalMode) ->
                database.playlists(PlaylistSortType.CREATE_DATE, true, ownerId, isLocalMode)
                    .map { list ->
                        list.count {
                            it.playlist.id != PlaylistEntity.LIKED_PLAYLIST_ID &&
                                    it.playlist.id != PlaylistEntity.DOWNLOADED_PLAYLIST_ID
                        }
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

                                                                       
    val memberSince: StateFlow<Long?> =
        context.dataStore.data.map { prefs ->
            val id = prefs[activeAccountIdKey]
            if (id.isNullOrBlank() || GuestManager.isGuest(id)) null
            else prefs[LocalAccountManager.createdAtKey(id)]
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    
    val isLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<LocalAccountManager.AuthError?>(null)

    fun clearError() { authError.value = null }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        runAuth(onSuccess) { LocalAccountManager.register(context.dataStore, name, email, password) }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        runAuth(onSuccess) { LocalAccountManager.login(context.dataStore, email, password) }
    }

    fun logout() {
        viewModelScope.launch { LocalAccountManager.logout(context.dataStore) }
    }

    private fun runAuth(
        onSuccess: () -> Unit,
        block: suspend () -> LocalAccountManager.AuthResult,
    ) {
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            authError.value = null
            try {
                when (val result = block()) {
                    is LocalAccountManager.AuthResult.Failure -> authError.value = result.error
                    is LocalAccountManager.AuthResult.Success -> {
                        adoptGuestData(result.accountId)
                        onSuccess()
                    }
                }
            } finally {
                isLoading.value = false
            }
        }
    }

                                                                           
    private suspend fun adoptGuestData(accountId: String) {
        val guestId = GuestManager.getOrCreateGuestId(context.dataStore)
        database.transaction {
            claimOrphanPlaylists(accountId)
            if (guestId.isNotBlank()) migratePlaylistOwner(fromOwnerId = guestId, toOwnerId = accountId)
        }
    }
}