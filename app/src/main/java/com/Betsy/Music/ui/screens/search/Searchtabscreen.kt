/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.rememberGuestId
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.utils.rememberPreference

@Composable
fun SearchTabScreen(
    query             : TextFieldValue,
    onQueryChange     : (TextFieldValue) -> Unit,
    onSearch          : (String) -> Unit,
    navController     : NavController,
    onDismiss         : () -> Unit,
    pureBlack         : Boolean,
    playerIsDismissed : Boolean,
) {
    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    var isStreaming by rememberPreference(StreamingModeKey, defaultValue = true)

    
    val context = LocalContext.current
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }
    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val guestId = rememberGuestId()
    val currentOwnerId = remember(prefs, guestId) {
        val activeId = prefs[activeAccountIdKey] ?: ""
        activeId.ifBlank { guestId }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when {
                    isStreaming -> stringResource(R.string.search_yt_music_header)
                    else -> stringResource(R.string.search_local_header)
                },
                fontSize = 13.sp, color = accentColor, fontWeight = FontWeight.Medium,
            )
            ModeChip(isStreaming = isStreaming, accentColor = accentColor, onToggle = { isStreaming = !isStreaming })
        }

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)),
            placeholder = {
                Text(
                    when {
                        isStreaming -> stringResource(R.string.search_yt_music)
                        else -> stringResource(R.string.search_local_placeholder)
                    },
                    color = adaptiveColor.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = adaptiveColor.copy(alpha = 0.55f))
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange(TextFieldValue("")) }) {
                            Icon(Icons.Default.Close, null, tint = adaptiveColor.copy(alpha = 0.55f))
                        }
                    }
                    IconButton(onClick = { isStreaming = !isStreaming }) {
                        Icon(
                            painter = painterResource(if (isStreaming) R.drawable.language else R.drawable.library_music),
                            contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor        = adaptiveColor,
                unfocusedTextColor      = adaptiveColor,
                focusedContainerColor   = adaptiveColor.copy(alpha = 0.08f),
                unfocusedContainerColor = adaptiveColor.copy(alpha = 0.05f),
                focusedBorderColor      = accentColor,
                unfocusedBorderColor    = adaptiveColor.copy(alpha = 0.20f),
                cursorColor             = accentColor,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (query.text.isNotEmpty()) onSearch(query.text) }),
        )

        Spacer(Modifier.height(8.dp))

        if (query.text.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SearchVinyl(accentColor = accentColor)
                Spacer(Modifier.height(40.dp))
                Text(stringResource(R.string.search_title), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = adaptiveColor)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isStreaming) stringResource(R.string.search_yt_music_subtitle)
                    else stringResource(R.string.search_local_subtitle),
                    fontSize = 15.sp, color = adaptiveColor.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(28.dp))
                ModeBanner(isStreaming = isStreaming, accentColor = accentColor, onToggle = { isStreaming = !isStreaming })
            }
        } else {
            Crossfade(targetState = isStreaming, label = "search_mode", modifier = Modifier.fillMaxSize()) { streaming ->
                if (streaming) {
                    OnlineSearchScreen(
                        query = query.text, onQueryChange = onQueryChange,
                        navController = navController, onSearch = onSearch, onDismiss = { },
                        ownerId = currentOwnerId,
                    )
                } else {
                    LocalSearchScreen(
                        query = query.text, navController = navController,
                        
                        
                        onDismiss = { }, pureBlack = pureBlack,
                        onQueryChange = onQueryChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChip(isStreaming: Boolean, accentColor: Color, onToggle: () -> Unit) {
    val tint  = if (isStreaming) accentColor else Color(0xFF4CAF50)
    val icon  = if (isStreaming) R.drawable.language else R.drawable.library_music
    val label = if (isStreaming) stringResource(R.string.lib_mode_streaming) else stringResource(R.string.lib_mode_local)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(13.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModeBanner(isStreaming: Boolean, accentColor: Color, onToggle: () -> Unit) {
    val tint     = if (isStreaming) accentColor else Color(0xFF4CAF50)
    val icon     = if (isStreaming) R.drawable.language else R.drawable.library_music
    val mainText = if (isStreaming) stringResource(R.string.search_banner_streaming) else stringResource(R.string.search_banner_local)
    val subText  = if (isStreaming) stringResource(R.string.search_banner_tap_local) else stringResource(R.string.search_banner_tap_streaming)
    Row(
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.09f))
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(22.dp))
        Column {
            Text(mainText, color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subText, color = tint.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun SearchVinyl(accentColor: Color) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    val transition = rememberInfiniteTransition(label = "vinyl")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    Box(
        modifier = Modifier.size(180.dp).rotate(rotation).clip(CircleShape).background(adaptiveColor.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(140.dp).clip(CircleShape).background(adaptiveColor.copy(alpha = 0.28f)))
        Box(
            modifier = Modifier.size(70.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Search, null, tint = accentColor, modifier = Modifier.size(34.dp))
        }
    }
}
