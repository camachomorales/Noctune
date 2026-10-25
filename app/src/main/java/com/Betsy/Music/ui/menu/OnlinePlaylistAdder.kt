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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.SongItem
import com.betsy.music.LocalDatabase
import com.betsy.music.R
import com.betsy.music.constants.DataSyncIdKey
import com.betsy.music.constants.ListThumbnailSize
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.Playlist
import com.betsy.music.db.entities.Song
import com.betsy.music.models.ItemsPage
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.ui.component.CreatePlaylistDialog
import com.betsy.music.ui.component.DefaultDialog
import com.betsy.music.ui.component.ListDialog
import com.betsy.music.ui.component.ListItem
import com.betsy.music.ui.component.PlaylistListItem
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.rememberIsLoggedIn
import com.betsy.music.utils.rememberPreference
import com.betsy.music.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun OnlinePlaylistAdder(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    songs: SnapshotStateList<Song>,
    onDismiss: () -> Unit,
    onProgressStart: (Boolean) -> Unit,
    onPercentageChange: (Int) -> Unit
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val viewStateMap = remember { mutableStateMapOf<String, ItemsPage?>() }
    var playlists by remember { mutableStateOf(emptyList<Playlist>()) }

    val isLoggedIn = rememberIsLoggedIn()
    val (dataSyncId) = rememberPreference(DataSyncIdKey, "")

    val (isStreaming) = rememberPreference(StreamingModeKey, true)
    val isLocalMode = !isStreaming
    val guestId = rememberGuestId()
    val currentOwnerId = if (isLoggedIn && dataSyncId.isNotBlank()) dataSyncId else guestId

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val songIds by remember { mutableStateOf<List<String>?>(null) }
    val duplicates by remember { mutableStateOf(emptyList<String>()) }

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
                            painter = painterResource(id = R.drawable.playlist_add),
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
                            onDismiss()
                            val songsTot = songs.count().toDouble()
                            var songsIdx = 0.toDouble()
                            onProgressStart(true)
                            songs.reversed().forEach { song ->
                                var allArtists = ""
                                song.artists.forEach { artist ->
                                    allArtists += " ${URLDecoder.decode(artist.name, StandardCharsets.UTF_8.toString())}"
                                }
                                val query = "${song.title} - $allArtists"
                                coroutineScope.launch {
                                    try {
                                        AudiusApi.search(query, AudiusApi.SearchFilter.FILTER_SONG)
                                            .onSuccess { result ->
                                                viewStateMap[AudiusApi.SearchFilter.FILTER_SONG.value] =
                                                    ItemsPage(result.items.distinctBy { it.id }, result.continuation)
                                                val itemsPage = viewStateMap.entries.first().value!!
                                                val firstSong = itemsPage.items[0] as SongItem
                                                val firstSongMedia = firstSong.toMediaMetadata()
                                                val ids = List(1) { firstSong.id }
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        database.insert(firstSongMedia)
                                                    } catch (e: Exception) {
                                                        Timber.tag("Exception inserting song in database:").e(e.toString())
                                                    }
                                                    database.addSongToPlaylist(playlist, ids)
                                                }
                                                viewStateMap.clear()
                                                songsIdx += 1
                                            }
                                            .onFailure {
                                                reportException(it)
                                                songsIdx += 1
                                            }
                                        if (songsIdx.toInt() == songsTot.toInt() - 1) onProgressStart(false)
                                        onPercentageChange(((songsIdx / songsTot) * 100).toInt())
                                    } catch (e: Exception) {
                                        Timber.tag("ERROR").v(e.toString())
                                    }
                                }
                            }
                        }
                    }
                )
            }

            item {
                ListItem(
                    modifier = Modifier.clickable {
                        coroutineScope.launch(Dispatchers.IO) {
                            onDismiss()
                            val songsTot = songs.count().toDouble()
                            var songsIdx = 0.toDouble()
                            onProgressStart(true)
                            songs.reversed().forEach { song ->
                                var allArtists = ""
                                song.artists.forEach { artist ->
                                    allArtists += " ${URLDecoder.decode(artist.name, StandardCharsets.UTF_8.toString())}"
                                }
                                val query = "${song.title} - $allArtists"
                                coroutineScope.launch {
                                    try {
                                        AudiusApi.search(query, AudiusApi.SearchFilter.FILTER_SONG)
                                            .onSuccess { result ->
                                                viewStateMap[AudiusApi.SearchFilter.FILTER_SONG.value] =
                                                    ItemsPage(result.items.distinctBy { it.id }, result.continuation)
                                                val itemsPage = viewStateMap.entries.first().value!!
                                                val firstSong = itemsPage.items[0] as SongItem
                                                val firstSongMedia = firstSong.toMediaMetadata()
                                                val firstSongEnt = firstSong.toMediaMetadata().toSongEntity()
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        database.insert(firstSongMedia)
                                                        database.query { update(firstSongEnt.toggleLike()) }
                                                    } catch (e: Exception) {
                                                        Timber.tag("Exception inserting song in database:").e(e.toString())
                                                    }
                                                }
                                                viewStateMap.clear()
                                                songsIdx += 1
                                            }
                                            .onFailure {
                                                reportException(it)
                                                songsIdx += 1
                                            }
                                        if (songsIdx.toInt() == songsTot.toInt() - 1) onProgressStart(false)
                                        onPercentageChange(((songsIdx / songsTot) * 100).toInt())
                                    } catch (e: Exception) {
                                        Timber.tag("ERROR").v(e.toString())
                                    }
                                }
                            }
                        }
                    },
                    title = stringResource(R.string.liked_songs),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(id = R.drawable.favorite),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                        )
                    },
                    trailingContent = {}
                )
            }

            item {
                Text(
                    text = stringResource(R.string.playlist_add_local_to_synced_note),
                    fontSize = TextUnit(12F, TextUnitType.Sp),
                    modifier = Modifier.padding(horizontal = 20.dp)
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