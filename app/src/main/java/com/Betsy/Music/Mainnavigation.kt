/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.AppBarHeight
import com.betsy.music.constants.DefaultOpenTabKey
import com.betsy.music.constants.MiniPlayerHeight
import com.betsy.music.constants.NavigationBarAnimationSpec
import com.betsy.music.constants.NavigationBarHeight
import com.betsy.music.constants.PauseSearchHistoryKey
import com.betsy.music.constants.PlayerBackgroundStyle
import com.betsy.music.constants.PlayerBackgroundStyleKey
import com.betsy.music.constants.SearchSource
import com.betsy.music.constants.SearchSourceKey
import com.betsy.music.constants.SlimNavBarKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.db.entities.SearchHistory
import com.betsy.music.extensions.toEnum
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.DownloadUtil
import com.betsy.music.playback.PlayerConnection
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.AvatarPreferenceManager
import com.betsy.music.ui.component.AvatarSelection
import com.betsy.music.ui.component.BottomSheetMenu
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.Lyrics
import com.betsy.music.ui.component.TopSearch
import com.betsy.music.ui.component.shimmer.ShimmerTheme
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.ui.player.MiniPlayer
import com.betsy.music.ui.player.QueueScreen
import com.betsy.music.ui.player.musicPlayerScreen
import com.betsy.music.ui.screens.Screens
import com.betsy.music.ui.screens.navigationBuilder
import com.betsy.music.ui.screens.search.LocalSearchScreen
import com.betsy.music.ui.screens.search.OnlineSearchScreen
import com.betsy.music.ui.screens.search.SearchTabScreen
import com.betsy.music.ui.screens.settings.NavigationTab
import com.betsy.music.ui.theme.ColorSaver
import com.betsy.music.ui.theme.DefaultThemeColor
import com.betsy.music.ui.theme.musicTheme
import com.betsy.music.ui.theme.extractThemeColor
import com.betsy.music.ui.utils.appBarScrollBehavior
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.ui.utils.resetHeightOffset
import com.betsy.music.utils.SyncUtils
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import com.betsy.music.utils.rememberSearchOwnerId
import com.betsy.music.constants.StreamingSource
import com.betsy.music.constants.StreamingSourceKey
import com.betsy.music.utils.reportException
import com.betsy.music.viewmodels.NewReleaseViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder



@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    playerConnection: PlayerConnection?,
    database: musicDatabase,
    downloadUtil: DownloadUtil,
    syncUtils: SyncUtils,
    latestVersionName: String,
    enableDynamicTheme: Boolean,
    useDarkTheme: Boolean,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var themeColor by rememberSaveable(stateSaver = ColorSaver) {
        mutableStateOf(DefaultThemeColor)
    }

    LaunchedEffect(playerConnection, enableDynamicTheme) {
        if (!enableDynamicTheme || playerConnection == null) {
            themeColor = DefaultThemeColor
            return@LaunchedEffect
        }
        playerConnection.service.currentMediaMetadata.collect { song ->
            themeColor = if (song != null) {
                withContext(Dispatchers.IO) {
                    val result = context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(song.thumbnailUrl)
                            .allowHardware(false)
                            .build()
                    )
                    (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor()
                        ?: DefaultThemeColor
                }
            } else DefaultThemeColor
        }
    }

    musicTheme(
        darkTheme = useDarkTheme,
        pureBlack = pureBlack,
        themeColor = DefaultThemeColor,
    ){
        MainNavigationContent(
            playerConnection = playerConnection,
            database = database,
            downloadUtil = downloadUtil,
            syncUtils = syncUtils,
            latestVersionName = latestVersionName,
            pureBlack = pureBlack,
            modifier = modifier,
        )
    }
}



@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavigationContent(
    playerConnection: PlayerConnection?,
    database: musicDatabase,
    downloadUtil: DownloadUtil,
    syncUtils: SyncUtils,
    latestVersionName: String,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val windowsInsets = WindowInsets.systemBars
    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val navigationItems = remember { Screens.MainScreens }
    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)

    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    val defaultOpenTab = remember {
        context.dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
    }
    val tabOpenedFromShortcut = remember {
        when ((context as? Activity)?.intent?.action) {
            MainActivity.ACTION_LIBRARY -> NavigationTab.LIBRARY
            else -> null
        }
    }

    val startDestination: String = when (tabOpenedFromShortcut ?: defaultOpenTab) {
        NavigationTab.HOME    -> Screens.Home.route
        NavigationTab.SEARCH  -> Screens.Search.route
        NavigationTab.LIBRARY -> Screens.Library.route
        else                  -> Screens.Home.route
    }

    val topLevelScreens = listOf(
        Screens.Home.route,
        Screens.Library.route,
        "settings",
    )

    
    val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var active by rememberSaveable { mutableStateOf(false) }
    val onActiveChange: (Boolean) -> Unit = { newActive ->
        active = newActive
        if (!newActive) {
            focusManager.clearFocus()
            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                onQueryChange(TextFieldValue())
            }
        }
    }
    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    val searchBarFocusRequester = remember { FocusRequester() }

    
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)

    
    val searchOwnerId = rememberSearchOwnerId()

    
    val navigateOnlineSearch: (String) -> Unit = { encoded ->
        navController.navigate("search/$encoded")
    }

    val onSearch: (String) -> Unit = { it ->
        if (it.isNotEmpty()) {
            onActiveChange(false)
            val encoded = URLEncoder.encode(it, "UTF-8")
            
            if (isStreamingMode) {
                navigateOnlineSearch(encoded)
            } else {
                navController.navigate("local_search/$encoded")
            }
            if (context.dataStore[PauseSearchHistoryKey] != true) {
                database.query { insert(SearchHistory(query = it, ownerId = searchOwnerId)) }
            }
        }
    }

    val onSearchFromTab: (String) -> Unit = { searchQuery ->
        if (searchQuery.isNotEmpty()) {
            onQueryChange(TextFieldValue(text = searchQuery, selection = TextRange(searchQuery.length)))
            val encoded = URLEncoder.encode(searchQuery, "UTF-8")
            
            if (isStreamingMode) {
                navigateOnlineSearch(encoded)
            } else {
                navController.navigate("local_search/$encoded")
            }
            if (context.dataStore[PauseSearchHistoryKey] != true) {
                database.query { insert(SearchHistory(query = searchQuery, ownerId = searchOwnerId)) }
            }
        }
    }

    var openSearchImmediately by remember {
        mutableStateOf((context as? Activity)?.intent?.action == MainActivity.ACTION_SEARCH)
    }

    val isOnSearchTab = navBackStackEntry?.destination?.route == Screens.Search.route
    val isOnPlayerScreen = navBackStackEntry?.destination?.route.let { it == "player" || it == "queue" }

    val shouldShowSearchBar = remember(active, navBackStackEntry) {
        active ||
                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                navBackStackEntry?.destination?.route?.startsWith("search/") == true ||
                navBackStackEntry?.destination?.route?.startsWith("local_search/") == true ||
                navBackStackEntry?.destination?.route?.startsWith("ia_search/") == true
    }
    val shouldShowNavigationBar = remember(navBackStackEntry, active) {
        navBackStackEntry?.destination?.route == null ||
                (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && !active)
    }
    val navigationBarHeight by animateDpAsState(
        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
        animationSpec = NavigationBarAnimationSpec,
        label = "",
    )

    val currentMediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata
            ?: kotlinx.coroutines.flow.MutableStateFlow(null)
    }.collectAsState()
    val hasActiveMedia = currentMediaMetadata != null

    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.0f))
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

            val isSearchOrLocalRoute = navBackStackEntry?.destination?.route?.let {
                it.startsWith("search/") ||
                        it.startsWith("local_search/") ||
                        it.startsWith("ia_search/") ||
                        it.startsWith("local_playlist/") ||
                        it.startsWith("auto_playlist/") ||
                        it.startsWith("top_playlist/")
            } == true

            val searchBarScrollBehavior = appBarScrollBehavior(
                canScroll = { !isSearchOrLocalRoute }
            )
            val topAppBarScrollBehavior = appBarScrollBehavior(
                canScroll = { !isSearchOrLocalRoute }
            )

            LaunchedEffect(navBackStackEntry) {
                val route = navBackStackEntry?.destination?.route
                if (route?.startsWith("search/") == true || route?.startsWith("local_search/") == true || route?.startsWith("ia_search/") == true) {
                    val searchQuery = withContext(Dispatchers.IO) {
                        val raw = navBackStackEntry?.arguments?.getString("query")!!
                        if (raw.contains("%")) raw else URLDecoder.decode(raw, "UTF-8")
                    }
                    onQueryChange(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
                } else if (navigationItems.fastAny { it.route == route }) {
                    if (route != Screens.Search.route) {
                        onQueryChange(TextFieldValue())
                    }
                }
                searchBarScrollBehavior.state.resetHeightOffset()
                topAppBarScrollBehavior.state.resetHeightOffset()
            }

            LaunchedEffect(active) {
                if (active) {
                    searchBarScrollBehavior.state.resetHeightOffset()
                    topAppBarScrollBehavior.state.resetHeightOffset()
                    searchBarFocusRequester.requestFocus()
                }
            }

            var sharedSong: SongItem? by remember { mutableStateOf(null) }
            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { intent ->
                    val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)
                        ?.toUri() ?: return@Consumer
                    when (val path = uri.pathSegments.firstOrNull()) {
                        "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                            if (playlistId.startsWith("OLAK5uy_")) {
                                coroutineScope.launch {
                                    AudiusApi.albumSongs(playlistId)
                                        .onSuccess { songs ->
                                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                                navController.navigate("album/$browseId")
                                            }
                                        }.onFailure { reportException(it) }
                                }
                            } else {
                                navController.navigate("online_playlist/$playlistId")
                            }
                        }
                        "browse" -> uri.lastPathSegment?.let { navController.navigate("album/$it") }
                        "channel", "c" -> uri.lastPathSegment?.let { navController.navigate("artist/$it") }
                        else -> when {
                            path == "watch" -> uri.getQueryParameter("v")
                            else -> null
                        }?.let { videoId ->
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) { AudiusApi.queue(listOf(videoId)) }
                                    .onSuccess {
                                        playerConnection?.playQueue(
                                            StreamingQueue(
                                                WatchEndpoint(videoId = it.firstOrNull()?.id),
                                                it.firstOrNull()?.toMediaMetadata()
                                            )
                                        )
                                    }.onFailure { reportException(it) }
                            }
                        }
                    }
                }
                (context as? ComponentActivity)?.addOnNewIntentListener(listener)
                onDispose {
                    (context as? ComponentActivity)?.removeOnNewIntentListener(listener)
                }
            }

            var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(navBackStackEntry) {
                val route = navBackStackEntry?.destination?.route
                shouldShowTopBar = !active && (
                        route == Screens.Home.route ||
                                route == Screens.Library.route
                        )
            }

            var showFullscreenLyrics by remember { mutableStateOf(false) }

            val playerAwareWindowInsets = remember(bottomInset, shouldShowNavigationBar, hasActiveMedia) {
                var bottom = bottomInset
                if (shouldShowNavigationBar) bottom += NavigationBarHeight
                if (hasActiveMedia) bottom += MiniPlayerHeight
                windowsInsets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    .add(WindowInsets(top = AppBarHeight, bottom = bottom))
            }

            CompositionLocalProvider(
                LocalDatabase provides database,
                LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                LocalPlayerConnection provides playerConnection,
                LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                LocalDownloadUtil provides downloadUtil,
                LocalShimmerTheme provides ShimmerTheme,
                LocalSyncUtils provides syncUtils,
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        val isSearchRoute = navBackStackEntry?.destination?.route?.let {
                            it.startsWith("search/") || it.startsWith("local_search/") || it.startsWith("ia_search/")
                        } == true
                        if ((active || isSearchRoute) && !isOnSearchTab && !isOnPlayerScreen) {
                            





                            TopSearch(
                                query = query,
                                onQueryChange = onQueryChange,
                                onSearch = onSearch,
                                active = active,
                                onActiveChange = onActiveChange,
                                placeholder = {
                                    Text(
                                        stringResource(
                                            when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }
                                        )
                                    )
                                },
                                leadingIcon = {
                                    com.betsy.music.ui.component.IconButton(
                                        onClick = {
                                            when {
                                                active -> onActiveChange(false)
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ->
                                                    navController.navigateUp()
                                                else -> onActiveChange(true)
                                            }
                                        },
                                        onLongClick = {
                                            when {
                                                active -> {}
                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ->
                                                    navController.backToMain()
                                                else -> {}
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painterResource(
                                                if (active || !navigationItems.fastAny {
                                                        it.route == navBackStackEntry?.destination?.route
                                                    }) R.drawable.arrow_back else R.drawable.search
                                            ),
                                            contentDescription = null,
                                            tint = com.betsy.music.ui.theme.rememberAdaptiveOnColor(),
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (active) {
                                            if (query.text.isNotEmpty()) {
                                                IconButton(onClick = { onQueryChange(TextFieldValue("")) }) {
                                                    Icon(
                                                        painterResource(R.drawable.close),
                                                        contentDescription = null,
                                                        tint = com.betsy.music.ui.theme.rememberAdaptiveOnColor(),
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    searchSource = if (searchSource == SearchSource.ONLINE)
                                                        SearchSource.LOCAL else SearchSource.ONLINE
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        when (searchSource) {
                                                            SearchSource.LOCAL -> R.drawable.library_music
                                                            SearchSource.ONLINE -> R.drawable.language
                                                        }
                                                    ),
                                                    contentDescription = stringResource(
                                                        when (searchSource) {
                                                            SearchSource.LOCAL -> R.string.search_online
                                                            SearchSource.ONLINE -> R.string.search_library
                                                        }
                                                    ),
                                                    tint = accentColor,
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .focusRequester(searchBarFocusRequester)
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth(),
                                focusRequester = searchBarFocusRequester
                            ) {
                                Crossfade(
                                    targetState = searchSource,
                                    label = "search_content_transition",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = if (hasActiveMedia) MiniPlayerHeight else 0.dp)
                                        .navigationBarsPadding(),
                                ) { currentSearchSource ->
                                    when (currentSearchSource) {
                                        SearchSource.LOCAL -> LocalSearchScreen(
                                            query = query.text,
                                            navController = navController,
                                            onDismiss = { onActiveChange(false) },
                                            pureBlack = pureBlack,
                                            onQueryChange = onQueryChange,
                                        )
                                        SearchSource.ONLINE -> OnlineSearchScreen(
                                            query = query.text,
                                            onQueryChange = onQueryChange,
                                            navController = navController,
                                            onSearch = { searchQuery ->
                                                try {
                                                    val encoded = URLEncoder.encode(searchQuery, "UTF-8")
                                                    if (isStreamingMode) {
                                                        navigateOnlineSearch(encoded)
                                                    } else {
                                                        navController.navigate("local_search/$encoded")
                                                    }
                                                    if (context.dataStore[PauseSearchHistoryKey] != true) {
                                                        database.query { insert(SearchHistory(query = searchQuery, ownerId = searchOwnerId)) }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("SearchNavigation", "Error: ${e.message}", e)
                                                }
                                            },
                                            onDismiss = { onActiveChange(false) },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isOnPlayerScreen) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AnimatedVisibility(
                                    visible = hasActiveMedia,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    MiniPlayer(
                                        onNavigateToPlayer = {
                                            navController.navigate("player") { launchSingleTop = true }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                NavigationBar(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(15.dp))
                                        .fillMaxWidth(),
                                    containerColor = Color.Transparent,
                                    contentColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor(),
                                    tonalElevation = 0.dp
                                ) {
                                    var lastTapTime by remember { mutableLongStateOf(0L) }
                                    var lastTappedRoute by remember { mutableStateOf<String?>(null) }

                                    navigationItems.fastForEach { screen ->
                                        val isSelected = when (screen) {
                                            Screens.Search -> isOnSearchTab
                                            else -> !isOnSearchTab && !active &&
                                                    navBackStackEntry?.destination?.hierarchy?.any {
                                                        it.route == screen.route
                                                    } == true
                                        }

                                        NavigationBarItem(
                                            selected = isSelected,
                                            icon = {
                                                Icon(
                                                    painter = painterResource(
                                                        if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                    ),
                                                    contentDescription = stringResource(screen.titleId),
                                                )
                                            },
                                            label = {
                                                if (!slimNav) {
                                                    Text(
                                                        text = stringResource(screen.titleId),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor   = accentColor,
                                                selectedTextColor   = accentColor,
                                                unselectedIconColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor().copy(alpha = 0.7f),
                                                unselectedTextColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor().copy(alpha = 0.7f),
                                                indicatorColor      = Color.Transparent
                                            ),
                                            onClick = {
                                                val now = System.currentTimeMillis()
                                                when (screen) {
                                                    Screens.Search -> {
                                                        lastTapTime = now
                                                        lastTappedRoute = screen.route
                                                        if (isOnSearchTab) {
                                                            navController.currentBackStackEntry
                                                                ?.savedStateHandle?.set("scrollToTop", true)
                                                        } else {
                                                            if (active) onActiveChange(false)
                                                            try { navigateToScreen(navController, screen) }
                                                            catch (e: Exception) { Log.e("Navigation", "Error", e) }
                                                        }
                                                    }
                                                    else -> {
                                                        lastTapTime = now
                                                        lastTappedRoute = screen.route
                                                        if (isSelected) {
                                                            navController.currentBackStackEntry
                                                                ?.savedStateHandle?.set("scrollToTop", true)
                                                            coroutineScope.launch {
                                                                try { searchBarScrollBehavior.state.resetHeightOffset() }
                                                                catch (e: Exception) { Log.e("ScrollBehavior", "Error") }
                                                            }
                                                        } else {
                                                            try { navigateToScreen(navController, screen) }
                                                            catch (e: Exception) { Log.e("Navigation", "Error", e) }
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        enterTransition = {
                            if (initialState.destination.route in topLevelScreens &&
                                targetState.destination.route in topLevelScreens
                            ) fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            else if (targetState.destination.route == "player")
                                slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) + fadeIn(tween(350))
                            else fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                    slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow))
                        },
                        exitTransition = {
                            if (initialState.destination.route in topLevelScreens &&
                                targetState.destination.route in topLevelScreens
                            ) fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            else if (targetState.destination.route == "player")
                                fadeOut(tween(200))
                            else fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                                    slideOutHorizontally(targetOffsetX = { -it / 5 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        popEnterTransition = {
                            if ((initialState.destination.route in topLevelScreens ||
                                        isSearchOrLocalRoute) &&
                                targetState.destination.route in topLevelScreens
                            ) fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            else if (initialState.destination.route == "player")
                                fadeIn(tween(200))
                            else fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                    slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(stiffness = Spring.StiffnessLow))
                        },
                        popExitTransition = {
                            if ((initialState.destination.route in topLevelScreens ||
                                        isSearchOrLocalRoute) &&
                                targetState.destination.route in topLevelScreens
                            ) fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            else if (initialState.destination.route == "player")
                                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350)) + fadeOut(tween(350))
                            else fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                                    slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        modifier = Modifier
                            .padding(
                                start  = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                end    = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                                top    = if (isSearchOrLocalRoute) 0.dp
                                else innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                            .nestedScroll(
                                if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    isSearchOrLocalRoute
                                ) searchBarScrollBehavior.nestedScrollConnection
                                else topAppBarScrollBehavior.nestedScrollConnection
                            )
                    ) {
                        navigationBuilder(navController, topAppBarScrollBehavior, latestVersionName)

                        composable(Screens.Search.route) {
                            SearchTabScreen(
                                query             = query,
                                onQueryChange     = onQueryChange,
                                onSearch          = onSearchFromTab,
                                navController     = navController,
                                onDismiss         = { navController.popBackStack() },
                                pureBlack         = pureBlack,
                                playerIsDismissed = !hasActiveMedia,
                            )
                        }
                        composable(
                            route = "player",
                            enterTransition = {
                                slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) + fadeIn(tween(350))
                            },
                            exitTransition = { fadeOut(tween(200)) },
                            popEnterTransition = { fadeIn(tween(200)) },
                            popExitTransition = {
                                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350)) + fadeOut(tween(350))
                            }
                        ) {
                            musicPlayerScreen(
                                onBackClick   = { navController.popBackStack() },
                                navController = navController,
                                onOpenQueue   = { navController.navigate("queue") { launchSingleTop = true } },
                                modifier      = Modifier.fillMaxSize()
                            )
                        }
                        composable(
                            route = "queue",
                            enterTransition = {
                                slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
                            },
                            exitTransition = { fadeOut(tween(200)) },
                            popEnterTransition = { fadeIn(tween(200)) },
                            popExitTransition = {
                                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300))
                            }
                        ) {
                            QueueScreen(
                                onBackClick = { navController.popBackStack() },
                                modifier    = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                BottomSheetMenu(
                    state = LocalMenuState.current,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                sharedSong?.let { song ->
                    playerConnection?.let {
                        Dialog(
                            onDismissRequest = { sharedSong = null },
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                        ) {
                            Surface(
                                modifier = Modifier.padding(24.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = AlertDialogDefaults.containerColor,
                                tonalElevation = AlertDialogDefaults.TonalElevation,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    StreamingSongMenu(
                                        song = song,
                                        navController = navController,
                                        onDismiss = { sharedSong = null },
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showFullscreenLyrics,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
                    exit  = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300))
                ) {
                    val conn = LocalPlayerConnection.current
                    val mediaMetadata by conn?.mediaMetadata?.collectAsState()
                        ?: return@AnimatedVisibility
                    if (mediaMetadata != null) {
                        Lyrics(
                            sliderPositionProvider = { null },
                            onNavigateBack = { showFullscreenLyrics = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                if (shouldShowSearchBar && openSearchImmediately) {
                    onActiveChange(true)
                    try {
                        delay(100)
                        searchBarFocusRequester.requestFocus()
                    } catch (_: Exception) { }
                    openSearchImmediately = false
                }
            }
        }
    }
}


private fun navigateToScreen(navController: NavHostController, screen: Screens) {
    navController.navigate(screen.route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}


@Composable
fun ProfileIconWithUpdateBadge(
    currentVersion: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
    var showUpdateBadge by remember { mutableStateOf(false) }
    val updatedOnClick = rememberUpdatedState(onProfileClick)

    val infiniteTransition = rememberInfiniteTransition(label = "badge_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    LaunchedEffect(currentVersion) {
        try {
            val latestVersion = withContext(Dispatchers.IO) { checkForUpdates() }
            showUpdateBadge = latestVersion?.let { isNewerVersion(it, currentVersion) } ?: false
        } catch (e: Exception) {
            Timber.tag("ProfileIcon").e("Error checking for updates: ${e.message}")
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                try { updatedOnClick.value() } catch (e: Exception) { e.printStackTrace() }
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (currentSelection) {
                is AvatarSelection.Custom -> AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                        .crossfade(true).error(R.drawable.person).placeholder(R.drawable.person).build(),
                    contentDescription = "Avatar personalizado",
                    modifier = modifier.size(28.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                is AvatarSelection.DiceBear -> AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data((currentSelection as AvatarSelection.DiceBear).url)
                        .crossfade(true).error(R.drawable.person).placeholder(R.drawable.person).build(),
                    contentDescription = "Avatar DiceBear",
                    modifier = modifier.size(28.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                else -> Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = "Avatar predeterminado",
                    modifier = modifier
                )
            }
        }

        if (showUpdateBadge) {
            Box(modifier = Modifier.align(Alignment.Center).size(28.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * alpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), shape = CircleShape)
                )
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = "Actualización disponible",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).align(Alignment.Center).scale(scale).alpha(alpha)
                )
            }
        }
    }
}