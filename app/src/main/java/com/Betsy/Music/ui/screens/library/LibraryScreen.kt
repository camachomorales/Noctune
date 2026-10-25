/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.Song
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.menu.SongMenu
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val isStreamingMode by rememberPreference(StreamingModeKey, defaultValue = true)
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    
    val playerConnection = LocalPlayerConnection.current
    val haptic           = LocalHapticFeedback.current
    val menuState        = LocalMenuState.current

    val isPlaying     by (playerConnection?.isPlaying
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val mediaMetadata by (playerConnection?.mediaMetadata
        ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()

    
    val strTitle        = stringResource(R.string.lib_title)
    val strModeStream   = stringResource(R.string.lib_mode_streaming)
    val strModeLocal    = stringResource(R.string.lib_mode_local)
    val strPlaylists    = stringResource(R.string.lib_nav_playlists)
    val strSongs        = stringResource(R.string.lib_nav_songs)
    val strAlbums       = stringResource(R.string.lib_nav_albums)
    val strArtists      = stringResource(R.string.lib_nav_artists)
    val strFavorites    = stringResource(R.string.lib_nav_favorites)
    val strHistory      = stringResource(R.string.lib_nav_history)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            )
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint               = accentColor,
                modifier           = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text       = strTitle,
            fontSize   = 36.sp,
            fontWeight = FontWeight.Bold,
            color      = adaptiveColor
        )

        Text(
            text     = if (isStreamingMode) strModeStream else strModeLocal,
            fontSize = 13.sp,
            color    = accentColor,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier            = Modifier.fillMaxWidth()
        ) {
            LibraryOptionCard(
                icon      = Icons.Default.QueueMusic,
                title     = strPlaylists,
                iconColor = accentColor,
                onClick   = { navController.navigate("library/playlists") }
            )
            LibraryOptionCard(
                icon      = Icons.Default.MusicNote,
                title     = strSongs,
                iconColor = accentColor,
                onClick   = { navController.navigate("library/songs") }
            )
            LibraryOptionCard(
                icon      = Icons.Default.Album,
                title     = strAlbums,
                iconColor = accentColor,
                onClick   = { navController.navigate("library/albums") }
            )
            LibraryOptionCard(
                icon      = Icons.Default.Person,
                title     = strArtists,
                iconColor = accentColor,
                onClick   = { navController.navigate("library/artists") }
            )
            LibraryOptionCard(
                icon      = Icons.Default.Favorite,
                title     = strFavorites,
                iconColor = Color(0xFFFF006E),
                onClick   = { navController.navigate("auto_playlist/liked") }
            )
            LibraryOptionCard(
                icon      = Icons.Default.History,
                title     = strHistory,
                iconColor = accentColor,
                onClick   = { navController.navigate("history") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        
        if (playerConnection != null) {
            LocalLibraryPreview(
                navController    = navController,
                playerConnection = playerConnection,
                isPlaying        = isPlaying,
                mediaMetadata    = mediaMetadata,
                haptic           = haptic,
                menuState        = menuState,
                viewModel        = viewModel,
                accentColor      = accentColor,
                isStreamingMode  = isStreamingMode
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryOptionCard(
    icon      : ImageVector,
    title     : String,
    iconColor : Color = Color(0xFF3b9cac),
    onClick   : () -> Unit,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(adaptiveColor.copy(alpha = 0.08f))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = title,
                tint               = iconColor,
                modifier           = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text       = title,
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = adaptiveColor,
            modifier   = Modifier.weight(1f)
        )
        Icon(
            imageVector        = Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = adaptiveColor.copy(alpha = 0.5f),
            modifier           = Modifier.size(26.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalLibraryPreview(
    navController    : NavController,
    playerConnection : com.betsy.music.playback.PlayerConnection,
    isPlaying        : Boolean,
    mediaMetadata    : com.betsy.music.models.MediaMetadata?,
    haptic           : androidx.compose.ui.hapticfeedback.HapticFeedback,
    menuState        : com.betsy.music.ui.component.MenuState,
    viewModel        : LibraryViewModel,
    accentColor      : Color = Color(0xFF3b9cac),
    isStreamingMode  : Boolean = false,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    val allRecent by viewModel.recentLocalSongs.collectAsState()
    
    val recentSongs = remember(allRecent, isStreamingMode) {
        allRecent.filter { it.song.id.startsWith("device_") != isStreamingMode }
    }
    if (recentSongs.isEmpty()) return

    val strQuickPicks = stringResource(R.string.lib_quick_picks)
    val strSeeAll     = stringResource(R.string.lib_see_all)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(text = strQuickPicks, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = adaptiveColor)
            Text(
                text     = strSeeAll,
                fontSize = 13.sp,
                color    = accentColor,
                modifier = Modifier.combinedClickable(
                    onClick     = { navController.navigate("library/songs") },
                    onLongClick = {}
                )
            )
        }
        recentSongs.take(5).forEach { song: Song ->
            LocalSongRow(
                song        = song,
                isActive    = song.id == mediaMetadata?.id,
                isPlaying   = isPlaying,
                onClick     = {
                    if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                    else playerConnection.playQueue(
                        ListQueue(
                            title = song.song.title,
                            items = recentSongs.map { it.toSafeMediaItem() },
                            startIndex = recentSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                        )
                    )
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                    }
                },
                accentColor = accentColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalSongRow(
    song       : Song,
    isActive   : Boolean,
    isPlaying  : Boolean,
    onClick    : () -> Unit,
    onLongClick: () -> Unit,
    accentColor: Color = Color(0xFF3b9cac),
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(adaptiveColor.copy(alpha = if (isActive) 0.14f else 0.06f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A3A4A)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = song.song.title.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = song.song.title,
                fontSize   = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color      = if (isActive) accentColor else adaptiveColor,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text     = song.artists.joinToString { it.name },
                fontSize = 13.sp,
                color    = adaptiveColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isActive) Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.height(8.dp))
}
   
                                                                   
                                                                             
                                                                                    
                                                                           
                                                  
   
private fun com.betsy.music.db.entities.Song.toSafeMediaItem(): MediaItem {
    return if (song.id.startsWith("device_")) {
        val numericId = song.id.removePrefix("device_").toLongOrNull() ?: 0L
        val uri = Uri.withAppendedPath(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            numericId.toString()
        )
        val extras = android.os.Bundle().apply {
            putString("mediaId", song.id)
            putBoolean("isLocal", true)
        }
        MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(uri)
            .setCustomCacheKey(song.id)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(extras)
                    .build()
            )
            .setTag(toMediaMetadata())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(artists.joinToString { it.name })
                    .setAlbumTitle(song.albumName)
                    .setArtworkUri(song.thumbnailUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    } else {
        toMediaItem()
    }
}