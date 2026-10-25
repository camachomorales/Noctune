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
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.betsy.music.BuildConfig
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.R
import com.betsy.music.constants.AccountNameKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.ui.component.AvatarPreferenceManager
import com.betsy.music.ui.component.AvatarSelection
import com.betsy.music.ui.component.IconButton
import com.betsy.music.utils.GuestManager
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.NewReleaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL


private val CardBg: Color
    @Composable get() = com.betsy.music.ui.theme.rememberAdaptiveOnColor().copy(alpha = 0.1f)

@Composable
private fun accentColor(): Color = com.betsy.music.ui.theme.rememberAccentColor()





@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName, PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VersionCard(uriHandler: UriHandler) {
    val context    = LocalContext.current
    val appVersion = remember { getAppVersion(context) }
    SettingsPainterCard(
        icon     = painterResource(R.drawable.info),
        title    = stringResource(R.string.Version),
        subtitle = appVersion,
        onClick  = { uriHandler.openUri("${com.betsy.music.utils.AppLinks.REPO_URL}/releases/latest") }
    )
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote  = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}





@Composable
fun SettingsPainterCard(
    icon          : Painter,
    title         : String,
    subtitle      : String?  = null,
    isHighlighted : Boolean  = false,
    onClick       : () -> Unit
) {
    val dynamicAccent = accentColor()
    val accentColor   = if (isHighlighted) Color(0xFFFFBE0B) else dynamicAccent
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = adaptiveColor)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 13.sp, color = adaptiveColor.copy(alpha = 0.6f))
            }
        }
        Icon(
            painter            = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint               = adaptiveColor.copy(alpha = 0.4f),
            modifier           = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsPainterCardWithToggle(
    icon      : Painter,
    title     : String,
    subtitle  : String?  = null,
    isEnabled : Boolean,
    onToggle  : (Boolean) -> Unit
) {
    val dynamicAccent = accentColor()
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(44.dp).clip(CircleShape).background(dynamicAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = icon, contentDescription = title, tint = dynamicAccent, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = adaptiveColor)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 13.sp, color = adaptiveColor.copy(alpha = 0.6f))
            }
        }
        Switch(
            checked         = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = adaptiveColor,
                checkedTrackColor   = dynamicAccent,
                uncheckedThumbColor = adaptiveColor,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    val dynamicAccent = accentColor()
    Text(
        text       = text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color      = dynamicAccent.copy(alpha = 0.8f),
        modifier   = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}





data class Contributor(
    val fullName  : String,
    val handle    : String,
    val role      : String,
    val githubUrl : String,
    val chips     : List<Pair<String, Color>> = emptyList()
)

@Composable
fun CreatorCard(uriHandler: UriHandler) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    val dynamicAccent = accentColor()
    val appName    = "Nombre de la App"
    val appTagline = "Tu descripción corta"
    val repoUrl    = com.betsy.music.utils.AppLinks.REPO_URL
    val contributors = listOf(
        Contributor(
            fullName  = "Nombre del Creador",
            handle    = "@handle_creador",
            role      = "Desarrollador Principal",
            githubUrl = "https://github.com/handle_creador",
            chips     = listOf(
                "Android Dev" to dynamicAccent,
                "Kotlin"      to Color(0xFF7F52FF),
                "Open Source" to Color(0xFF4CAF50)
            )
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(dynamicAccent.copy(alpha = 0.20f), Color(0xFF0A6EBD).copy(alpha = 0.14f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(dynamicAccent.copy(alpha = 0.45f), Color(0xFF0A6EBD).copy(alpha = 0.25f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp).clip(RoundedCornerShape(18.dp))
                .background(dynamicAccent.copy(alpha = 0.18f))
                .border(1.dp, dynamicAccent.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter            = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                tint               = dynamicAccent,
                modifier           = Modifier.size(40.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = appName,    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = adaptiveColor)
            Text(text = appTagline, fontSize = 12.sp, color = adaptiveColor.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
        Box(
            modifier = Modifier.fillMaxWidth(0.55f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, dynamicAccent.copy(alpha = 0.55f), Color.Transparent)))
        )
        Text(text = stringResource(R.string.team), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = dynamicAccent.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth())
        contributors.forEach { contributor ->
            ContributorRow(contributor = contributor, accent = dynamicAccent, uriHandler = uriHandler)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(adaptiveColor.copy(alpha = 0.07f))
                .border(1.dp, adaptiveColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { uriHandler.openUri(repoUrl) }
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(R.drawable.info), contentDescription = null, tint = adaptiveColor.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.view_repository), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = adaptiveColor.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun ContributorRow(contributor: Contributor, accent: Color, uriHandler: UriHandler) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(adaptiveColor.copy(alpha = 0.05f))
            .clickable { uriHandler.openUri(contributor.githubUrl) }
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val initials = contributor.fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent, Color(0xFF0A6EBD)), start = Offset(0f, 0f), end = Offset(60f, 60f))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = contributor.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = adaptiveColor)
            Text(text = contributor.role,     fontSize = 11.sp, color = adaptiveColor.copy(alpha = 0.5f))
            if (contributor.chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    contributor.chips.forEach { (label, chipColor) ->
                        CreatorChip(label = label, accent = chipColor)
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = contributor.handle, fontSize = 11.sp, color = accent.copy(alpha = 0.8f))
            Icon(painter = painterResource(R.drawable.arrow_forward), contentDescription = null, tint = adaptiveColor.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CreatorChip(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = accent, fontWeight = FontWeight.Medium)
    }
}





@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    latestVersion : Long,
    navController : NavController,
) {
    val uriHandler = LocalUriHandler.current
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    val context       = LocalContext.current
    val isLoggedIn = com.betsy.music.utils.rememberIsLoggedIn()
    var isStreamingMode by rememberPreference(StreamingModeKey, defaultValue = true)







    val strStreamingTitle    = stringResource(R.string.streaming_mode_title)
    val strStreamingSubtitle = stringResource(R.string.streaming_mode_subtitle)
    val strLocalTitle        = stringResource(R.string.local_mode_title)
    val strLocalSubtitle     = stringResource(R.string.local_mode_subtitle)
    val strPlaybackSection   = stringResource(R.string.section_playback_mode)
    val strAppTagline        = stringResource(R.string.app_tagline)

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding      = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text(
                text       = stringResource(R.string.settings),
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = adaptiveColor,
                modifier   = Modifier.padding(vertical = 8.dp)
            )
        }


        if (!isLoggedIn) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFBE0B).copy(alpha = 0.10f))
                        .border(1.dp, Color(0xFFFFBE0B).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { navController.navigate("settings/account") }
                        .padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠", fontSize = 18.sp)
                    Column {
                        Text(
                            text       = stringResource(R.string.no_account_mode),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFFFFBE0B)
                        )
                        Text(
                            text     = stringResource(R.string.uninstall_warning_short),
                            fontSize = 11.sp,
                            color    = adaptiveColor.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }


        item { SectionLabel(strPlaybackSection) }
        item {
            SettingsPainterCardWithToggle(
                icon      = painterResource(if (isStreamingMode) R.drawable.wifi_proxy else R.drawable.offline),
                title     = if (isStreamingMode) strStreamingTitle else strLocalTitle,
                subtitle  = if (isStreamingMode) strStreamingSubtitle else strLocalSubtitle,
                isEnabled = isStreamingMode,
                onToggle  = { isStreamingMode = it }
            )
        }

        item { Spacer(Modifier.height(4.dp)) }


        item { SectionLabel(stringResource(R.string.general_settings)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingsPainterCard(icon = painterResource(R.drawable.palette),  title = stringResource(R.string.appearance),      onClick = { navController.navigate("settings/appearance") })
                SettingsPainterCard(icon = painterResource(R.drawable.person),   title = stringResource(R.string.account),          onClick = { navController.navigate("settings/account") })
                SettingsPainterCard(icon = painterResource(R.drawable.language), title = stringResource(R.string.content),          onClick = { navController.navigate("settings/content") })
                SettingsPainterCard(icon = painterResource(R.drawable.play),     title = stringResource(R.string.player_and_audio), onClick = { navController.navigate("settings/player") })
                SettingsPainterCard(icon = painterResource(R.drawable.storage),  title = stringResource(R.string.storage),          onClick = { navController.navigate("settings/storage") })
                SettingsPainterCard(icon = painterResource(R.drawable.security), title = stringResource(R.string.privacy),          onClick = { navController.navigate("settings/privacy") })
                SettingsPainterCard(icon = painterResource(R.drawable.restore),  title = stringResource(R.string.backup_restore),   onClick = { navController.navigate("settings/backup_restore") })
                SettingsPainterCard(icon = painterResource(R.drawable.info),     title = stringResource(R.string.about),            onClick = { navController.navigate("settings/about") })
            }
        }

        item { VersionCard(uriHandler) }
    }
}