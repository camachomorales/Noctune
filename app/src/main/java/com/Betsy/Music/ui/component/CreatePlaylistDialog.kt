/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.betsy.music.ui.theme.rememberAccentColor
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.betsy.audius.compat.AudiusApi
import androidx.compose.ui.platform.LocalContext
import com.betsy.music.LocalDatabase
import com.betsy.music.R
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.runtime.collectAsState
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    @Suppress("UNUSED_PARAMETER") allowSyncing: Boolean = true, 
) {
    val database       = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    
    val accent   = rememberAccentColor()
    val text     = rememberAdaptiveOnColor()
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    
    val dialogBg = Color(0xFF0A2F3D).copy(alpha = 0.97f)

    
    val context = LocalContext.current
    val guestId = rememberGuestId()
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }

    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val ownerId = remember(prefs, guestId) {
        val activeId = prefs[activeAccountIdKey] ?: ""
        activeId.ifBlank { guestId }
    }

    
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)

    var name by remember { mutableStateOf(TextFieldValue(initialTextFieldValue ?: "")) }
    var mixed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        runCatching { focusRequester.requestFocus() }
    }

    fun create(playlistName: String) {
        coroutineScope.launch(Dispatchers.IO) {
            
            
            withContext(NonCancellable) {
                database.transaction {
                    insert(
                        PlaylistEntity(
                            name         = playlistName,
                            browseId     = null,               
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable   = true,
                            ownerId      = ownerId,            
                            isLocalMode  = !isStreamingMode,   
                            isMixed      = mixed,              
                        )
                    )
                }
            }
        }
    }

    val isValid = name.text.isNotBlank()
    fun submit() {
        if (!isValid) return
        val n = name.text.trim()
        onDismiss()
        create(n)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = dialogBg,
        shape            = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.create_playlist),
                color = text,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.auth_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = text,
                        unfocusedTextColor   = text,
                        focusedBorderColor   = accent,
                        unfocusedBorderColor = text.copy(alpha = 0.3f),
                        focusedLabelColor    = accent,
                        unfocusedLabelColor  = text.copy(alpha = 0.6f),
                        cursorColor          = accent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(text.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lista mixta",
                            color = text,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Streaming + canciones del celular",
                            color = text.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Switch(
                        checked = mixed,
                        onCheckedChange = { mixed = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor    = onAccent,
                            checkedTrackColor    = accent,
                            uncheckedThumbColor  = text.copy(alpha = 0.7f),
                            uncheckedTrackColor  = text.copy(alpha = 0.15f),
                            uncheckedBorderColor = text.copy(alpha = 0.3f),
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor         = accent,
                    contentColor           = onAccent,
                    disabledContainerColor = accent.copy(alpha = 0.25f),
                    disabledContentColor   = text.copy(alpha = 0.4f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(android.R.string.ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel), color = text.copy(alpha = 0.7f))
            }
        },
    )
}