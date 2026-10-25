/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.betsy.music.ui.theme.rememberAccentColor
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import com.betsy.music.viewmodels.NewReleaseViewModel
import com.betsy.music.R
import com.betsy.music.constants.AccountEmailKey
import com.betsy.music.constants.AccountNameKey
import com.betsy.music.utils.GuestManager
import com.betsy.music.utils.LocalAccountManager
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.rememberIsLoggedIn
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.AccountViewModel
import java.util.Date

private val Pink = Color(0xFFFF006E)

                                                                                            
private data class AccountColors(
    val accent: Color,      
    val onAccent: Color,    
    val text: Color,        
    val dialogBg: Color,    
)

@Composable
private fun rememberAccountColors(): AccountColors {
    val accent = rememberAccentColor()
    val text = rememberAdaptiveOnColor()
    val isLightText = text.luminance() > 0.5f
    return AccountColors(
        accent = accent,
        onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White,
        text = text,
        
        dialogBg = if (isLightText) lerp(Color(0xFF101418), accent, 0.15f)
                   else lerp(Color(0xFFFFFFFF), accent, 0.10f),
    )
}

                                                                                    
@Composable
fun AccountContent(
    navController: NavController? = null,
    viewModel: AccountViewModel = hiltViewModel(),
    newReleaseViewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val c = rememberAccountColors()
    val hasNewReleases by newReleaseViewModel.hasNewReleases.collectAsState()
    val isLoggedIn = rememberIsLoggedIn()
    val guestId = rememberGuestId()
    val (accountName) = rememberPreference(AccountNameKey, "")
    val (accountEmail) = rememberPreference(AccountEmailKey, "")

    val songsPlayed by viewModel.songsPlayed.collectAsState()
    val listenedMillis by viewModel.listenedMillis.collectAsState()
    val favoritesCount by viewModel.favoritesCount.collectAsState()
    val playlistsCount by viewModel.playlistsCount.collectAsState()
    val memberSince by viewModel.memberSince.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var showLogin by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }

    val displayName = when {
        !isLoggedIn -> stringResource(R.string.guest)
        accountName.isNotBlank() -> accountName
        accountEmail.isNotBlank() -> accountEmail.substringBefore("@")
        else -> stringResource(R.string.account)
    }
    val subtitle = when {
        isLoggedIn -> accountEmail
        guestId.isNotBlank() -> stringResource(R.string.session_id, GuestManager.shortId(guestId))
        else -> stringResource(R.string.no_account_mode)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.text.copy(alpha = 0.1f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            AvatarDisplay(
                size        = 64.dp,
                showBorder  = true,
                borderColor = c.accent,
                borderWidth = 2.dp
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.text)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, fontSize = 14.sp, color = c.text.copy(alpha = 0.7f))
                }
            }
        }

        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.text.copy(alpha = 0.1f))
                .clickable {
                    newReleaseViewModel.markNewReleasesAsSeen()
                    navController?.navigate("new_release")
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.notification_on),
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (hasNewReleases) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4444))
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.new_release_albums),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = c.text.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }

        
        if (!isLoggedIn) {
            Button(
                onClick = { viewModel.clearError(); showLogin = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(painterResource(R.drawable.login), null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.auth_sign_in), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.clearError(); showRegister = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text),
                border = BorderStroke(1.dp, c.accent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.auth_create_account), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9E00).copy(alpha = 0.2f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠", fontSize = 18.sp, color = Color(0xFFFF9E00))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.auth_guest_warning),
                    fontSize = 13.sp, lineHeight = 18.sp, color = c.text.copy(alpha = 0.9f)
                )
            }
        }

        
        Text(stringResource(R.string.auth_statistics), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.text)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(c, Modifier.weight(1f), painterResource(R.drawable.music_note),
                songsPlayed.toString(), stringResource(R.string.auth_songs_played))
            StatCard(c, Modifier.weight(1f), painterResource(R.drawable.schedule),
                formatListenTime(listenedMillis), stringResource(R.string.auth_time_listened))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(c, Modifier.weight(1f), painterResource(R.drawable.favorite),
                favoritesCount.toString(), stringResource(R.string.auth_favorites))
            StatCard(c, Modifier.weight(1f), painterResource(R.drawable.queue_music),
                playlistsCount.toString(), stringResource(R.string.playlists))
        }

        
        if (isLoggedIn) {
            Text(stringResource(R.string.auth_account_info), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.text)
            if (accountEmail.isNotBlank()) InfoRow(c, stringResource(R.string.auth_email), accountEmail)
            memberSince?.let { InfoRow(c, stringResource(R.string.auth_member_since), formatDate(it)) }

            OutlinedButton(
                onClick = { showLogout = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Pink),
                border = BorderStroke(1.dp, Pink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(painterResource(R.drawable.logout), null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.logout), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    
    val errorText = authError?.let { authErrorText(it) }

    if (showLogin) {
        LoginDialog(
            c = c,
            isLoading = isLoading,
            errorText = errorText,
            onDismiss = { showLogin = false; viewModel.clearError() },
            onSubmit = { email, password ->
                viewModel.login(email, password) { showLogin = false }
            }
        )
    }
    if (showRegister) {
        RegisterDialog(
            c = c,
            isLoading = isLoading,
            errorText = errorText,
            onDismiss = { showRegister = false; viewModel.clearError() },
            onSubmit = { name, email, password ->
                viewModel.register(name, email, password) { showRegister = false }
            }
        )
    }
    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            containerColor = c.dialogBg,
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.auth_sign_out_title), color = c.text, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.auth_sign_out_message), color = c.text.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = { showLogout = false; viewModel.logout() }) {
                    Text(stringResource(R.string.logout), color = Pink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) {
                    Text(stringResource(R.string.auth_cancel), color = c.text.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun authErrorText(error: LocalAccountManager.AuthError): String = when (error) {
    LocalAccountManager.AuthError.NAME_EMPTY -> stringResource(R.string.auth_error_name)
    LocalAccountManager.AuthError.INVALID_EMAIL -> stringResource(R.string.auth_error_email_invalid)
    LocalAccountManager.AuthError.PASSWORD_TOO_SHORT ->
        stringResource(R.string.auth_error_password_short, LocalAccountManager.MIN_PASSWORD_LENGTH)
    LocalAccountManager.AuthError.EMAIL_TAKEN -> stringResource(R.string.auth_error_email_taken)
    LocalAccountManager.AuthError.WRONG_CREDENTIALS -> stringResource(R.string.auth_error_wrong_credentials)
}



@Composable
private fun fieldColors(c: AccountColors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = c.text,
    unfocusedTextColor = c.text,
    focusedBorderColor = c.accent,
    unfocusedBorderColor = c.text.copy(alpha = 0.3f),
    focusedLabelColor = c.accent,
    unfocusedLabelColor = c.text.copy(alpha = 0.6f),
    cursorColor = c.accent
)

@Composable
private fun PasswordField(c: AccountColors, value: String, onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.auth_password)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            androidx.compose.material3.IconButton(onClick = { show = !show }) {
                Icon(
                    if (show) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = stringResource(R.string.auth_toggle_password),
                    tint = c.text.copy(alpha = 0.6f)
                )
            }
        },
        colors = fieldColors(c),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DialogTitle(c: AccountColors, title: String, subtitle: String) {
    Column {
        Text(title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = c.text.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}

@Composable
private fun ErrorLine(text: String?) {
    if (text != null) Text(text, color = Pink, fontSize = 13.sp)
}

@Composable
private fun LoginDialog(
    c: AccountColors,
    isLoading: Boolean,
    errorText: String?,
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.dialogBg,
        shape = RoundedCornerShape(20.dp),
        title = { DialogTitle(c, stringResource(R.string.auth_sign_in), stringResource(R.string.auth_welcome_back)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(stringResource(R.string.auth_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors(c), modifier = Modifier.fillMaxWidth()
                )
                PasswordField(c, password) { password = it }
                ErrorLine(errorText)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = c.onAccent, strokeWidth = 2.dp)
                else Text(stringResource(R.string.auth_sign_in), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.auth_cancel), color = c.text.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun RegisterDialog(
    c: AccountColors,
    isLoading: Boolean,
    errorText: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, password: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.dialogBg,
        shape = RoundedCornerShape(20.dp),
        title = { DialogTitle(c, stringResource(R.string.auth_create_account), stringResource(R.string.auth_join_today)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.auth_name)) },
                    singleLine = true, colors = fieldColors(c), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(stringResource(R.string.auth_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors(c), modifier = Modifier.fillMaxWidth()
                )
                PasswordField(c, password) { password = it }
                ErrorLine(errorText)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, email, password) },
                enabled = name.isNotBlank() && email.isNotBlank() &&
                        password.length >= LocalAccountManager.MIN_PASSWORD_LENGTH && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = c.onAccent, strokeWidth = 2.dp)
                else Text(stringResource(R.string.auth_create), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.auth_cancel), color = c.text.copy(alpha = 0.7f))
            }
        }
    )
}



@Composable
private fun StatCard(c: AccountColors, modifier: Modifier, icon: Painter, value: String, label: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.text.copy(alpha = 0.1f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = c.accent, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = c.text.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoRow(c: AccountColors, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.text.copy(alpha = 0.05f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = c.text.copy(alpha = 0.7f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text)
    }
}

                                        
private fun formatListenTime(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours <= 0L -> "$minutes min"
        hours < 100L -> "${hours}h ${minutes}m"
        else -> "${hours}h"
    }
}

private fun formatDate(timestamp: Long): String =
    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(Date(timestamp))