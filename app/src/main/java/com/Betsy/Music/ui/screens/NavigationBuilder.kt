/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.utils.rememberPreference
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.betsy.music.BuildConfig
import com.betsy.music.ui.screens.artist.ArtistItemsScreen
import com.betsy.music.ui.screens.artist.ArtistScreen
import com.betsy.music.ui.screens.artist.ArtistSongsScreen
import com.betsy.music.ui.screens.library.CachePlaylistScreen
import com.betsy.music.ui.screens.library.LibraryAlbumsScreen
import com.betsy.music.ui.screens.library.LibraryArtistsScreen
import com.betsy.music.ui.screens.library.LibraryPlaylistsScreen
import com.betsy.music.ui.screens.library.LibrarySongsScreen
import com.betsy.music.ui.player.QueueScreen
import com.betsy.music.ui.player.musicPlayerScreen
import com.betsy.music.ui.screens.playlist.AutoPlaylistScreen
import com.betsy.music.ui.screens.playlist.LocalPlaylistScreen
import com.betsy.music.ui.screens.playlist.OnlinePlaylistScreen
import com.betsy.music.ui.screens.playlist.TopPlaylistScreen
import com.betsy.music.ui.screens.search.OnlineSearchResult
import com.betsy.music.ui.screens.search.LocalSearchResult
import com.betsy.music.ui.screens.search.InternetArchiveItemScreen
import com.betsy.music.ui.screens.search.InternetArchiveSearchResult
import com.betsy.music.ui.screens.settings.AboutScreen
import com.betsy.music.ui.screens.settings.AccountSettings
import com.betsy.music.ui.screens.settings.AppearanceSettings
import com.betsy.music.ui.screens.settings.BackupAndRestore
import com.betsy.music.ui.screens.settings.ContentSettings
import com.betsy.music.ui.screens.settings.PlayerSettings
import com.betsy.music.ui.screens.settings.PrivacySettings
import com.betsy.music.ui.screens.settings.SettingsScreen
import com.betsy.music.ui.screens.settings.StorageSettings

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(Screens.Library.route) {
        LibraryScreen(navController)
    }

    
    composable("player") {
        musicPlayerScreen(
            onBackClick   = { navController.navigateUp() },
            navController = navController,
            onOpenQueue   = { navController.navigate("queue") { launchSingleTop = true } },
        )
    }

    
    composable("queue") {
        QueueScreen(onBackClick = { navController.navigateUp() })
    }

    
    composable("local_songs") {
        LibrarySongsScreen(
            navController = navController,
            onDeselect = { navController.navigateUp() },
        )
    }

    
    composable("library/playlists") {
        LibraryPlaylistsScreen(
            navController = navController,
            filterContent = {},
        )
    }
    composable("library/songs") {
        LibrarySongsScreen(
            navController = navController,
            onDeselect = { navController.navigateUp() },
        )
    }
    composable("library/albums") {
        LibraryAlbumsScreen(
            navController = navController,
            onDeselect = { navController.navigateUp() },
        )
    }
    composable("library/artists") {
        LibraryArtistsScreen(
            navController = navController,
            onDeselect = { navController.navigateUp() },
        )
    }

    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }

    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") { type = NavType.StringType },
        ),
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true)
                fadeOut(tween(200))
            else
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true)
                fadeIn(tween(250))
            else
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
        },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        
        val isStreamingMode by rememberPreference(StreamingModeKey, defaultValue = true)
        if (!isStreamingMode) {
            
            androidx.compose.runtime.LaunchedEffect(Unit) { navController.popBackStack() }
        } else {
            OnlineSearchResult(navController)
        }
    }

    
    composable(
        route = "local_search/{query}",
        arguments = listOf(
            navArgument("query") { type = NavType.StringType },
        ),
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = {
            if (targetState.destination.route?.startsWith("local_search/") == true)
                fadeOut(tween(200))
            else
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("local_search/") == true)
                fadeIn(tween(250))
            else
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
        },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        
        
        
        LocalSearchResult(navController)
    }
    
    composable(
        route = "ia_search/{query}",
        arguments = listOf(navArgument("query") { type = NavType.StringType }),
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = {
            if (targetState.destination.route?.startsWith("ia_search/") == true)
                fadeOut(tween(200))
            else
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("ia_search/") == true)
                fadeIn(tween(250))
            else
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
        },
        popExitTransition = { fadeOut(tween(200)) },
    ) { backStackEntry ->
        InternetArchiveSearchResult(navController, backStackEntry.arguments?.getString("query").orEmpty().replace('+', ' '))
    }
    composable(
        route = "ia_item/{identifier}",
        arguments = listOf(navArgument("identifier") { type = NavType.StringType }),
    ) { backStackEntry ->
        InternetArchiveItemScreen(navController, backStackEntry.arguments?.getString("identifier").orEmpty())
    }
    composable(
        route = "album/{albumId}",
        arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getString("artistId")!!
        if (artistId.startsWith("LA")) {
            ArtistSongsScreen(navController, scrollBehavior)
        } else {
            ArtistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") { type = NavType.StringType },
            navArgument("browseId") { type = NavType.StringType; nullable = true },
            navArgument("params") { type = NavType.StringType; nullable = true },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(navArgument("playlist") { type = NavType.StringType }),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(navArgument("playlist") { type = NavType.StringType }),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments = listOf(navArgument("top") { type = NavType.StringType }),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "streaming_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") { type = NavType.StringType; nullable = true },
            navArgument("params") { type = NavType.StringType; nullable = true },
        ),
    ) {
        StreamingBrowseScreen(navController)
    }

    composable("settings") {
        val latestVersion by mutableLongStateOf(BuildConfig.VERSION_CODE.toLong())
        SettingsScreen(latestVersion, navController)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
    }
    composable("settings/account") {
        AccountSettings(navController, scrollBehavior)
    }
    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }
}