/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.betsy.audius.compat.AudiusApi
import com.betsy.music.LocalDatabase
import com.betsy.music.R
import com.betsy.music.constants.ListThumbnailSize
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.Playlist
import com.betsy.music.ui.component.CreatePlaylistDialog
import com.betsy.music.ui.component.DefaultDialog
import com.betsy.music.ui.component.ListDialog
import com.betsy.music.ui.component.ListItem
import com.betsy.music.ui.component.PlaylistListItem
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    onGetSong: suspend (Playlist) -> List<String>,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf(emptyList<Playlist>()) }

    
    
    
    
    val context = LocalContext.current
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }
    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val guestId = rememberGuestId()
    val currentOwnerId = remember(prefs, guestId) {
        val activeId = prefs[activeAccountIdKey] ?: ""
        activeId.ifBlank { guestId }
    }

    val (isStreaming) = rememberPreference(StreamingModeKey, true)
    val isLocalMode = !isStreaming

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var songIds by remember { mutableStateOf<List<String>?>(null) }
    var duplicates by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(currentOwnerId, isLocalMode) {
        database.editablePlaylistsByCreateDateAsc(currentOwnerId, isLocalMode).collect {
            playlists = it.asReversed()
        }
    }

    if (isVisible) {
        ListDialog(onDismiss = onDismiss) {
            item {
                ListItem(
                    title = stringResource(R.string.create_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    },
                    modifier = Modifier.clickable { showCreatePlaylistDialog = true }
                )
            }

            items(playlists) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    modifier = Modifier.clickable {
                        selectedPlaylist = playlist
                        coroutineScope.launch(Dispatchers.IO) {
                            if (songIds == null) songIds = onGetSong(playlist)
                            duplicates = database.playlistDuplicates(playlist.id, songIds!!)
                            if (duplicates.isNotEmpty()) {
                                showDuplicateDialog = true
                            } else {
                                onDismiss()
                                database.addSongToPlaylist(playlist, songIds!!)
                                playlist.playlist.browseId?.let { plist ->
                                    songIds?.forEach { AudiusApi.addToPlaylist(plist, it) }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    if (showDuplicateDialog) {
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(onClick = {
                    showDuplicateDialog = false
                    onDismiss()
                    database.transaction {
                        addSongToPlaylist(selectedPlaylist!!, songIds!!.filter { !duplicates.contains(it) })
                    }
                }) { Text(stringResource(R.string.skip_duplicates)) }

                TextButton(onClick = {
                    showDuplicateDialog = false
                    onDismiss()
                    database.transaction { addSongToPlaylist(selectedPlaylist!!, songIds!!) }
                }) { Text(stringResource(R.string.add_anyway)) }

                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = { showDuplicateDialog = false }
        ) {
            Text(
                text = if (duplicates.size == 1)
                    stringResource(R.string.duplicates_description_single)
                else
                    stringResource(R.string.duplicates_description_multiple, duplicates.size),
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}