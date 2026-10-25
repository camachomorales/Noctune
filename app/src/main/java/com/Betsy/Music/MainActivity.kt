/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.betsy.music.constants.DarkModeKey
import com.betsy.music.constants.DisableScreenshotKey
import com.betsy.music.constants.DynamicThemeKey
import com.betsy.music.constants.PureBlackKey
import com.betsy.music.constants.StopmusicOnTaskClearKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.playback.DownloadUtil
import com.betsy.music.playback.musicService
import com.betsy.music.playback.musicService.musicBinder
import com.betsy.music.playback.PlayerConnection
import com.betsy.music.ui.component.LocaleManager
import com.betsy.music.ui.component.SwitchPreference
import com.betsy.music.ui.screens.settings.DarkMode
import com.betsy.music.ui.theme.DefaultThemeColor
import com.betsy.music.ui.theme.musicTheme
import com.betsy.music.utils.SyncUtils
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import com.betsy.music.models.toMediaMetadata
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: musicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is musicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    override fun onStart() {
        super.onStart()
        startService(Intent(this, musicService::class.java))
        bindService(
            Intent(this, musicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopmusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, musicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.getInstance(newBase).applyLocaleToContext(newBase)
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
       

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        intent?.let { handlevideoIdIntent(it) }

        setContent {

            var showNoctuneWelcome by remember {
                mutableStateOf(true)
            }

            LaunchedEffect(Unit) {
                delay(2500)
                showNoctuneWelcome = false
            }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }

            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }

            
            musicTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = DefaultThemeColor,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    
                    MainNavigation(
                        playerConnection = playerConnection,
                        database = database,
                        downloadUtil = downloadUtil,
                        syncUtils = syncUtils,
                        latestVersionName = latestVersionName,
                        enableDynamicTheme = enableDynamicTheme,
                        useDarkTheme = useDarkTheme,
                        pureBlack = pureBlack,
                        modifier = Modifier.fillMaxSize()
                    )

                    
                    if (showNoctuneWelcome) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF071F38)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                NoctuneLogo(
                                    modifier = Modifier.size(240.dp)
                                )

                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.size(24.dp)
                                )

                                androidx.compose.material3.Text(
                                    text = "Noctune",
                                    color = Color(0xFF55BBD6),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.size(8.dp)
                                )

                                androidx.compose.material3.Text(
                                    text = "Tu música, tu espacio.",
                                    color = Color(0xFFB5DDE8),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    private fun handlevideoIdIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)
            ?.toUri() ?: return
        when {
            uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
            else -> null
        }?.let { videoId ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    com.betsy.audius.compat.AudiusApi.queue(listOf(videoId))
                }.onSuccess {
                    playerConnection?.playQueue(
                        com.betsy.music.playback.queues.StreamingQueue(
                            com.betsy.audius.compat.models.WatchEndpoint(videoId = it.firstOrNull()?.id),
                            it.firstOrNull()?.toMediaMetadata()
                        )
                    )
                }.onFailure {
                    com.betsy.music.utils.reportException(it)
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.betsy.music.action.SEARCH"
        const val ACTION_EXPLORE = "com.betsy.music.action.EXPLORE"
        const val ACTION_LIBRARY = "com.betsy.music.action.LIBRARY"
    }
}



val LocalDatabase =
    androidx.compose.runtime.staticCompositionLocalOf<musicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    androidx.compose.runtime.staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    androidx.compose.runtime.compositionLocalOf<androidx.compose.foundation.layout.WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil =
    androidx.compose.runtime.staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils =
    androidx.compose.runtime.staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }



@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val checkNotificationPermission = remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (!isGranted) {
            Log.d("NotificationPermission", "Permiso de notificaciones denegado")
        }
    }

    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SwitchPreference(
        title = { Text(stringResource( R.string.notification)) },
        icon = {
            Icon(
                painter = painterResource(
                    id = if (permissionGranted) R.drawable.notification_on
                    else R.drawable.notification_off
                ),
                contentDescription = stringResource(
                    if (permissionGranted) R.string.notifications_enabled
                    else R.string.notifications_disabled
                )
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            when {
                checked && !permissionGranted -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                }
                !checked && permissionGranted -> {
                    openNotificationSettings(context)
                }
            }
        }
    )
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("NotificationSettings", "No se pudo abrir configuración de notificaciones", e)
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}



                                                                                      
suspend fun checkForUpdates(): String? = null

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}
















