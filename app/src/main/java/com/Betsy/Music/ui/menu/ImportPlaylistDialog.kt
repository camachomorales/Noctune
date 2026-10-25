/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.menu

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.betsy.music.LocalDatabase
import com.betsy.music.R
import com.betsy.music.constants.DataSyncIdKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.music.ui.component.TextFieldDialog
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.rememberIsLoggedIn
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun ImportPlaylistDialog(
    isVisible: Boolean,
    onGetSong: suspend () -> List<String>,
    playlistTitle: String,
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()


    val (dataSyncId)      = rememberPreference(DataSyncIdKey, "")
    val isLoggedIn = rememberIsLoggedIn()
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)
    val guestId   = rememberGuestId()
    val ownerId   = if (isLoggedIn && dataSyncId.isNotBlank()) dataSyncId else guestId

    val textFieldValue by remember { mutableStateOf(TextFieldValue(text = playlistTitle)) }
    var songIds by remember {
        mutableStateOf<List<String>?>(null)
    }

    if (isVisible) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(text = stringResource(R.string.import_playlist)) },
            initialTextFieldValue = textFieldValue,
            autoFocus = false,
            onDismiss = onDismiss,
            onDone = { finalName ->
                val newPlaylist = PlaylistEntity(
                    name        = finalName,
                    ownerId     = ownerId,
                    isLocalMode = !isStreamingMode,
                )
                database.query { insert(newPlaylist) }

                coroutineScope.launch(Dispatchers.IO) {
                    val playlist = database.playlist(newPlaylist.id).firstOrNull()

                    if (playlist != null) {
                        songIds = onGetSong()
                        database.addSongToPlaylist(playlist, songIds!!)
                    }

                    onDismiss()
                }
            }
        )
    }
}