/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.db.entities.Song
import com.betsy.music.extensions.tryOrNull
import com.betsy.music.ui.component.IconButton
import com.betsy.music.ui.component.PreferenceEntry
import com.betsy.music.ui.component.SettingsGeneralCategory
import com.betsy.music.ui.component.SettingsPage
import com.betsy.music.ui.menu.OnlinePlaylistAdder
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.ui.utils.formatFileSize
import com.betsy.music.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("LogNotTimber")
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerCache = LocalPlayerConnection.current?.service?.playerCache
    val (accentColorInt) = com.betsy.music.utils.rememberPreference(
        com.betsy.music.constants.AccentColorKey, defaultValue = 0xFF3b9cac.toInt()
    )
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by remember { mutableStateOf(false) }
    var isProgressStarted by remember { mutableStateOf(false) }
    var progressPercentage by remember { mutableIntStateOf(0) }

    
    var playerCacheSize by remember { mutableLongStateOf(tryOrNull { playerCache?.cacheSpace } ?: 0L) }
    var isClearing by remember { mutableStateOf(false) }

    val animatedPlayerCacheSize by animateFloatAsState(
        targetValue = if (playerCacheSize > 0) 1f else 0f,
        label = "playerCacheProgress"
    )

    
    LaunchedEffect(playerCache) {
        while (true) {
            delay(1000)
            playerCacheSize = tryOrNull { playerCache?.cacheSpace } ?: 0L
        }
    }

    
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri)
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(context, uri)
            }
        }

    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val result = viewModel.importPlaylistFromCsv(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }

    val importM3uLauncherOnline =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val result = viewModel.loadM3UOnline(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }

    SettingsPage(
        title = stringResource(R.string.backup_restore),
        navController = navController,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.backup_restore),
            items = listOf(
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.backup)) },
                    icon = { Icon(painterResource(R.drawable.backup), null) },
                    description = stringResource(R.string.backup_description),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupLauncher.launch(
                            "${context.getString(R.string.app_name)}_${
                                LocalDateTime.now().format(formatter)
                            }.backup"
                        )
                    }
                )},
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.restore)) },
                    icon = { Icon(painterResource(R.drawable.restore), null) },
                    description = stringResource(R.string.restore_description),
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/octet-stream"))
                    }
                )}
            )
        )
    }

    
    OnlinePlaylistAdder(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { newVal -> isProgressStarted = newVal },
        onPercentageChange = { newPercentage -> progressPercentage = newPercentage }
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    if (isProgressStarted) {
        MinimalLoadingOverlay(progress = progressPercentage)
    }
}


@Composable
private fun MinimalLoadingOverlay(progress: Int) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(adaptiveColor.copy(alpha = if (adaptiveColor == Color.Black) 0.85f else 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp,
                color = com.betsy.music.ui.theme.rememberAccentColor(),
                trackColor = com.betsy.music.ui.theme.accentProgressTrackColor()
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = if (adaptiveColor == Color.Black) Color.White else Color.Black
            )
            Text(
                text = stringResource(R.string.processing_songs),
                style = MaterialTheme.typography.bodyMedium,
                color = (if (adaptiveColor == Color.Black) Color.White else Color.Black).copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MinimalInfoDialog(
    icon: Painter,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val (accentColorInt) = com.betsy.music.utils.rememberPreference(
        com.betsy.music.constants.AccentColorKey, defaultValue = 0xFF3b9cac.toInt()
    )
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = accentColor
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.understood))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun MinimalConfirmDialog(
    icon: Painter,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (accentColorInt) = com.betsy.music.utils.rememberPreference(
        com.betsy.music.constants.AccentColorKey, defaultValue = 0xFF3b9cac.toInt()
    )
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = accentColor
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)

    intent?.let {
        val componentName = it.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.setPackage(context.packageName)

        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}