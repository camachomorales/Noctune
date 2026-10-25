/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */



package com.betsy.music.ui.player

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.extensions.toggleRepeatMode
import com.betsy.music.utils.rememberPreference
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import kotlinx.coroutines.flow.flowOf




@Composable
fun musicPlayerScreen(
    onBackClick  : () -> Unit,
    navController: NavController,
    onOpenQueue  : () -> Unit = {},
    modifier     : Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database         = LocalDatabase.current

    
    var isStreamingMode by rememberPreference(StreamingModeKey, defaultValue = true)

    val isPlaying       by playerConnection.isPlaying.collectAsState()
    val playbackState   by playerConnection.playbackState.collectAsState()
    val mediaMetadata   by playerConnection.mediaMetadata.collectAsState()
    val currentSong     by playerConnection.currentSong.collectAsState(initial = null)
    val repeatMode      by playerConnection.repeatMode.collectAsState()
    val canSkipNext     by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val shuffleEnabled  by playerConnection.shuffleModeEnabled.collectAsState()
    val isLoading        = playbackState == Player.STATE_BUFFERING
    val currentFormat   by playerConnection.currentFormat.collectAsState(initial = null)

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(playbackState) {
        if (playbackState == Player.STATE_READY) {
            while (true) {
                kotlinx.coroutines.delay(200)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration.coerceAtLeast(0L)
            }
        }
    }

    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor   = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val textColor     = rememberAdaptiveOnColor()
    val secondaryText = textColor.copy(alpha = 0.6f)

    val songTitle  = mediaMetadata?.title
    val songArtist = mediaMetadata?.artists?.joinToString { it.name }
    val songId     = mediaMetadata?.id

    
    val artUri = mediaMetadata?.thumbnailUrl
        ?: currentSong?.album?.thumbnailUrl
        ?: currentSong?.song?.thumbnailUrl

    
    val hqArtUri = remember(artUri) {
        artUri?.replace(Regex("=w\\d+-h\\d+(-.*)?$"), "=w720-h720-l90-rj") ?: artUri
    }

    
    var imageLoaded by remember(hqArtUri) { mutableStateOf(false) }

    val albumFromMetadata = mediaMetadata?.album?.title?.takeIf { it.isNotBlank() }
    val albumFromSong     = currentSong?.song?.albumName?.takeIf { it.isNotBlank() }
    val albumFromRelation = currentSong?.album?.title?.takeIf { it.isNotBlank() }
    val albumId           = mediaMetadata?.album?.id ?: currentSong?.song?.albumId
    val albumFromDb by remember(albumId) {
        if (!albumId.isNullOrBlank()) database.album(albumId) else flowOf(null)
    }.collectAsState(initial = null)

    
    val songFromDb by remember(songId) {
        if (!songId.isNullOrBlank()) database.song(songId) else flowOf(null)
    }.collectAsState(initial = null)

    
    val isLiked = songFromDb?.song?.liked ?: currentSong?.song?.liked ?: false

    var playlistName by remember(songId) { mutableStateOf<String?>(null) }
    LaunchedEffect(songId) {
        playlistName = null
        if (!songId.isNullOrBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val maps = database.playlistSongMaps(songId)
                val realPlaylistId = maps.map { it.playlistId }.firstOrNull { id ->
                    id != com.betsy.music.db.entities.PlaylistEntity.LIKED_PLAYLIST_ID &&
                            id != com.betsy.music.db.entities.PlaylistEntity.DOWNLOADED_PLAYLIST_ID
                }
                if (realPlaylistId != null) {
                    database.playlist(realPlaylistId).collect { playlist ->
                        playlistName = playlist?.playlist?.name?.takeIf { it.isNotBlank() }
                    }
                }
            }
        }
    }

    val songAlbum = albumFromMetadata ?: albumFromSong ?: albumFromRelation
    ?: albumFromDb?.album?.title?.takeIf { it.isNotBlank() }

    val (contextPrefix, contextIcon, contextLabel) = when {
        songAlbum != null           -> Triple(stringResource(R.string.context_album),        Icons.Default.Album,      songAlbum)
        playlistName != null        -> Triple(stringResource(R.string.context_playlist),     Icons.Default.QueueMusic, playlistName!!)
        !songArtist.isNullOrBlank() -> Triple(stringResource(R.string.context_artist),       Icons.Default.Person,     songArtist!!)
        else                        -> Triple(stringResource(R.string.context_playing_from), Icons.Default.MusicNote,  "Streaming")
    }

    val shuffleColor = if (shuffleEnabled) accentColor else secondaryText
    val repeatColor  = if (repeatMode != Player.REPEAT_MODE_OFF) accentColor else secondaryText

    var showMenu     by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    
    if (showDetailsDialog) {
        AlertDialog(
            properties       = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            containerColor   = Color(0xFF0D2D3A),
            icon = {
                Icon(
                    painter            = painterResource(R.drawable.info),
                    contentDescription = null,
                    tint               = accentColor,
                )
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text(stringResource(android.R.string.ok), color = accentColor)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title)   to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id)     to mediaMetadata?.id,
                        "Itag"                                to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type)    to currentFormat?.mimeType,
                        stringResource(R.string.codecs)       to currentFormat?.codecs,
                        stringResource(R.string.bitrate)      to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate)  to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness)     to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume)       to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size)    to currentFormat?.contentLength?.let {
                            Formatter.formatShortFileSize(context, it)
                        },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.55f),
                        )
                        Text(
                            text     = displayText,
                            style    = MaterialTheme.typography.titleMedium,
                            color    = Color.White,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = {
                                    clipboardManager.setText(AnnotatedString(displayText))
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                }
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            
            Row(
                modifier              = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector        = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.close_player),
                        tint               = textColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }

                
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier              = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isStreamingMode) accentColor.copy(alpha = 0.15f)
                            else Color(0xFF2A5C3A).copy(alpha = 0.4f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector        = if (isStreamingMode) Icons.Default.Cloud else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint               = if (isStreamingMode) accentColor else Color(0xFF4CAF50),
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text       = if (isStreamingMode) stringResource(R.string.lib_mode_streaming) else stringResource(R.string.lib_mode_local),
                        fontSize   = 11.sp,
                        color      = if (isStreamingMode) accentColor else Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick  = {
                        
                        
                        
                        val song = songFromDb?.song
                        if (song != null) {
                            database.query { update(song.toggleLike()) }
                        } else {
                            mediaMetadata?.let { meta ->
                                database.query {
                                    insert(meta, com.betsy.music.db.entities.SongEntity::toggleLike)
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector        = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.action_like),
                        tint               = if (isLiked) Color(0xFFFF6B6B) else textColor,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }

            
            Box(
                modifier         = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                            .background(
                                Brush.linearGradient(listOf(
                                    Color(0xFF0B5A6B).copy(alpha = 0.5f),
                                    Color(0xFF051E26).copy(alpha = 0.5f)
                                ))
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        
                        if (!imageLoaded) {
                            Icon(
                                imageVector        = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint               = Color.White.copy(alpha = 0.3f),
                                modifier           = Modifier.size(140.dp)
                            )
                        }

                        
                        if (!hqArtUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(hqArtUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.album_art),
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp)),
                                contentScale       = ContentScale.Crop,
                                onSuccess          = { imageLoaded = true },
                                onError            = { imageLoaded = false }
                            )
                        }

                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                        startY = 300f
                                    )
                                )
                                .padding(start = 16.dp, end = 16.dp, bottom = 56.dp, top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text       = songTitle ?: stringResource(R.string.no_song),
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textColor,
                                textAlign  = TextAlign.Center,
                                maxLines   = 2
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = songArtist ?: stringResource(R.string.unknown_artist),
                                fontSize  = 14.sp,
                                color     = secondaryText,
                                textAlign = TextAlign.Center,
                                maxLines  = 1
                            )
                        }

                        
                        if (isLoading) {
                            Box(
                                modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color       = accentColor,
                                        modifier    = Modifier.size(48.dp),
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "Cargando...",
                                        color      = Color.White,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    
                    Button(
                        onClick = {
                            if (playbackState == Player.STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                        modifier  = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 38.dp)
                            .size(76.dp),
                        shape     = CircleShape,
                        colors    = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        elevation = ButtonDefaults.buttonElevation(8.dp, 4.dp),
                        enabled   = !isLoading
                    ) {
                        if (isLoading)
                            CircularProgressIndicator(
                                modifier    = Modifier.size(32.dp),
                                color       = Color(0xFF051E26),
                                strokeWidth = 3.dp
                            )
                        else
                            Icon(
                                imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.pause_action) else stringResource(R.string.play),
                                tint               = Color(0xFF051E26),
                                modifier           = Modifier.size(36.dp)
                            )
                    }
                }
            }

            Spacer(Modifier.height(52.dp))

            
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick  = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = shuffleColor, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick  = { playerConnection.seekToPrevious() },
                    modifier = Modifier.size(36.dp),
                    enabled  = canSkipPrevious && !isLoading
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.skip_previous),
                        contentDescription = stringResource(R.string.previous_action),
                        tint               = if (!canSkipPrevious || isLoading) textColor.copy(alpha = 0.3f) else textColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(48.dp))
                IconButton(
                    onClick  = { playerConnection.seekToNext() },
                    modifier = Modifier.size(36.dp),
                    enabled  = canSkipNext && !isLoading
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.next_action),
                        tint               = if (!canSkipNext || isLoading) textColor.copy(alpha = 0.3f) else textColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick  = { playerConnection.player.toggleRepeatMode() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Repeat, "Repeat", tint = repeatColor, modifier = Modifier.size(20.dp))
                        if (repeatMode == Player.REPEAT_MODE_ONE)
                            Text("1", fontSize = 7.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            
            PlayerProgressBar(
                currentPosition = position,
                duration        = duration,
                onSeek          = { playerConnection.player.seekTo(it) },
                accentColor     = accentColor
            )

            
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(contextIcon, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    contextPrefix.uppercase(),
                    fontSize      = 10.sp,
                    color         = textColor.copy(alpha = 0.5f),
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    contextLabel,
                    fontSize   = 13.sp,
                    color      = textColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1
                )
            }

            Spacer(Modifier.height(8.dp))

            
            Row(
                modifier              = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier            = Modifier.padding(start = 8.dp)
                ) {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, "Menu", tint = textColor, modifier = Modifier.size(24.dp))
                        }

                        DropdownMenu(
                            expanded         = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor   = Color(0xFF0A2F3D),
                            shape            = RoundedCornerShape(16.dp),
                            modifier         = Modifier.width(280.dp)
                        ) {

                            
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier              = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Speed, null,
                                        tint     = if (currentSpeed != 1.0f) accentColor else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Velocidad",
                                        fontSize = 12.sp,
                                        color    = if (currentSpeed != 1.0f) accentColor else Color.White.copy(alpha = 0.5f)
                                    )
                                    if (currentSpeed != 1.0f)
                                        Text(
                                            "${currentSpeed}×",
                                            fontSize   = 11.sp,
                                            color      = accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                }
                                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
                                speeds.chunked(5).forEach { rowSpeeds ->
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowSpeeds.forEach { speed ->
                                            val sel = currentSpeed == speed
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (sel) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                                                    .clickable { currentSpeed = speed; playerConnection.player.setPlaybackSpeed(speed) }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    if (speed == 1.0f) "1×" else "${speed}×",
                                                    fontSize   = 12.sp,
                                                    color      = if (sel) accentColor else Color.White.copy(alpha = 0.7f),
                                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines   = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier              = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier         = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isStreamingMode) Color(0xFF1A3A4A)
                                                    else Color(0xFF1A3A2A)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector        = if (isStreamingMode) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                                                contentDescription = null,
                                                tint               = if (isStreamingMode) Color(0xFF4CAF50) else accentColor,
                                                modifier           = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text  = if (isStreamingMode) stringResource(R.string.switch_to_local) else stringResource(R.string.switch_to_streaming),
                                                color = Color.White, fontSize = 14.sp
                                            )
                                            Text(
                                                text  = if (isStreamingMode)
                                                    stringResource(R.string.mode_hint_to_local)
                                                else
                                                    stringResource(R.string.mode_hint_to_streaming),
                                                color    = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    isStreamingMode = !isStreamingMode
                                    showMenu = false
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.QueueMusic, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.view_queue), color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onOpenQueue()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Share, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.share), color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    songId?.let { id ->
                                        val intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type   = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${songTitle.orEmpty()} - ${songArtist.orEmpty()}")
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    }
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Album, null, tint = if (albumId != null) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.go_to_album), color = if (albumId != null) Color.White else Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    albumId?.let { id ->
                                        showMenu = false
                                        navController.navigate("album/$id")
                                    }
                                },
                                enabled = albumId != null
                            )
                            DropdownMenuItem(
                                text = {
                                    val hasArtist = mediaMetadata?.artists?.any { it.id != null } == true
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Person, null, tint = if (hasArtist) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.go_to_artist), color = if (hasArtist) Color.White else Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    mediaMetadata?.artists?.firstOrNull { it.id != null }?.id?.let { artistId ->
                                        showMenu = false
                                        navController.navigate("artist/$artistId")
                                    }
                                },
                                enabled = mediaMetadata?.artists?.any { it.id != null } == true
                            )
                        }
                    }

                    HorizontalDivider(
                        color     = Color.White.copy(alpha = 0.15f),
                        thickness = 1.dp,
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    IconButton(onClick = { showDetailsDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Info, "Info", tint = textColor, modifier = Modifier.size(24.dp))
                    }
                }

                
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier            = Modifier.padding(end = 8.dp)
                ) { }
            }
        }
    }
}


@Composable
fun PlayerProgressBar(
    currentPosition : Long,
    duration        : Long,
    onSeek          : (Long) -> Unit,
    accentColor     : Color
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onSeek((duration * (offset.x / size.width).coerceIn(0f, 1f)).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onSeek((duration * (change.position.x / size.width).coerceIn(0f, 1f)).toLong())
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().align(Alignment.Center).height(4.dp)) {
                val cy = size.height / 2f
                drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, cy), Offset(size.width, cy),            4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(accentColor,                   Offset(0f, cy), Offset(size.width * progress, cy), 4.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(accentColor, 8.dp.toPx(), Offset(size.width * progress, cy))
            }
        }
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(playerFormatTime(currentPosition), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            Text(playerFormatTime(duration),        fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
        }
    }
}


private fun playerFormatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val hours        = totalSeconds / 3600
    val minutes      = (totalSeconds % 3600) / 60
    val seconds      = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}









































private val previewAccentColor = Color(0xFF3b9cac)




@Composable
private fun musicPlayerScreenStateless(
    songTitle       : String,
    songArtist      : String,
    artUri          : String?,
    isPlaying       : Boolean,
    isLoading       : Boolean,
    isLiked         : Boolean,
    isStreamingMode : Boolean,
    shuffleEnabled  : Boolean,
    repeatMode      : Int,
    canSkipNext     : Boolean,
    canSkipPrevious : Boolean,
    currentPosition : Long,
    duration        : Long,
    accentColor     : Color,
    contextPrefix   : String,
    contextLabel    : String,
    contextIcon     : androidx.compose.ui.graphics.vector.ImageVector,
    onBackClick     : () -> Unit,
    modifier        : Modifier = Modifier,
) {
    val textColor     = Color.White
    val secondaryText = Color.White.copy(alpha = 0.6f)
    val shuffleColor  = if (shuffleEnabled) accentColor else secondaryText
    val repeatColor   = if (repeatMode != Player.REPEAT_MODE_OFF) accentColor else secondaryText

    var showMenu     by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var modeFlag     by remember { mutableStateOf(isStreamingMode) }

    val hqArtUri = remember(artUri) {
        artUri?.replace(Regex("=w\\d+-h\\d+(-.*)?$"), "=w720-h720-l90-rj") ?: artUri
    }
    var imageLoaded by remember(hqArtUri) { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            
            Row(
                modifier              = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, stringResource(R.string.close),
                        tint     = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier              = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (modeFlag) accentColor.copy(alpha = 0.15f)
                            else Color(0xFF2A5C3A).copy(alpha = 0.4f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector        = if (modeFlag) Icons.Default.Cloud else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint               = if (modeFlag) accentColor else Color(0xFF4CAF50),
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text       = if (modeFlag) stringResource(R.string.lib_mode_streaming) else stringResource(R.string.lib_mode_local),
                        fontSize   = 11.sp,
                        color      = if (modeFlag) accentColor else Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector        = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.action_like),
                        tint               = if (isLiked) Color(0xFFFF6B6B) else textColor,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }

            
            Box(
                modifier         = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                            .background(
                                Brush.linearGradient(listOf(
                                    Color(0xFF0B5A6B).copy(alpha = 0.5f),
                                    Color(0xFF051E26).copy(alpha = 0.5f)
                                ))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!imageLoaded) {
                            Icon(
                                Icons.Default.MusicNote, null,
                                tint     = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(140.dp)
                            )
                        }
                        if (!hqArtUri.isNullOrEmpty()) {
                            AsyncImage(
                                model              = ImageRequest.Builder(context)
                                    .data(hqArtUri).crossfade(true).build(),
                                contentDescription = stringResource(R.string.album_art),
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp)),
                                contentScale       = ContentScale.Crop,
                                onSuccess          = { imageLoaded = true },
                                onError            = { imageLoaded = false }
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                        startY = 300f
                                    )
                                )
                                .padding(start = 16.dp, end = 16.dp, bottom = 56.dp, top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text       = songTitle,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color      = textColor,
                                textAlign  = TextAlign.Center,
                                maxLines   = 2
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = songArtist,
                                fontSize  = 14.sp,
                                color     = secondaryText,
                                textAlign = TextAlign.Center,
                                maxLines  = 1
                            )
                        }
                        if (isLoading) {
                            Box(
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color       = accentColor,
                                        modifier    = Modifier.size(48.dp),
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "Cargando...",
                                        color      = Color.White,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    
                    Button(
                        onClick   = {},
                        modifier  = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 38.dp)
                            .size(76.dp),
                        shape     = CircleShape,
                        colors    = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        elevation = ButtonDefaults.buttonElevation(8.dp, 4.dp),
                        enabled   = !isLoading
                    ) {
                        Icon(
                            imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.pause_action) else stringResource(R.string.play),
                            tint               = Color(0xFF051E26),
                            modifier           = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(52.dp))

            
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Shuffle, "Shuffle",
                        tint     = shuffleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick  = {},
                    modifier = Modifier.size(36.dp),
                    enabled  = canSkipPrevious
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.skip_previous),
                        contentDescription = stringResource(R.string.previous_action),
                        tint               = if (!canSkipPrevious) textColor.copy(alpha = 0.3f) else textColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(48.dp))
                IconButton(
                    onClick  = {},
                    modifier = Modifier.size(36.dp),
                    enabled  = canSkipNext
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.next_action),
                        tint               = if (!canSkipNext) textColor.copy(alpha = 0.3f) else textColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Repeat, "Repeat",
                            tint     = repeatColor,
                            modifier = Modifier.size(20.dp)
                        )
                        if (repeatMode == Player.REPEAT_MODE_ONE)
                            Text("1", fontSize = 7.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            
            PlayerProgressBar(
                currentPosition = currentPosition,
                duration        = duration,
                onSeek          = {},
                accentColor     = accentColor
            )

            
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(contextIcon, null,
                    tint     = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    contextPrefix.uppercase(),
                    fontSize      = 10.sp,
                    color         = textColor.copy(alpha = 0.5f),
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    contextLabel,
                    fontSize   = 13.sp,
                    color      = textColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1
                )
            }

            Spacer(Modifier.height(8.dp))

            
            Row(
                modifier              = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier            = Modifier.padding(start = 8.dp)
                ) {
                    Box {
                        IconButton(
                            onClick  = { showMenu = !showMenu },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, "Menu",
                                tint     = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded         = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor   = Color(0xFF0A2F3D),
                            shape            = RoundedCornerShape(16.dp),
                            modifier         = Modifier.width(280.dp)
                        ) {
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier              = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Speed, null,
                                        tint     = if (currentSpeed != 1.0f) accentColor else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Velocidad",
                                        fontSize = 12.sp,
                                        color    = if (currentSpeed != 1.0f) accentColor else Color.White.copy(alpha = 0.5f)
                                    )
                                    if (currentSpeed != 1.0f)
                                        Text(
                                            "${currentSpeed}×",
                                            fontSize   = 11.sp,
                                            color      = accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                }
                                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
                                speeds.chunked(5).forEach { rowSpeeds ->
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowSpeeds.forEach { speed ->
                                            val sel = currentSpeed == speed
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (sel) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                                                    .clickable { currentSpeed = speed }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    if (speed == 1.0f) "1×" else "${speed}×",
                                                    fontSize   = 12.sp,
                                                    color      = if (sel) accentColor else Color.White.copy(alpha = 0.7f),
                                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines   = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                color    = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier              = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (modeFlag) Color(0xFF1A3A4A) else Color(0xFF1A3A2A)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector        = if (modeFlag) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                                                contentDescription = null,
                                                tint               = if (modeFlag) Color(0xFF4CAF50) else accentColor,
                                                modifier           = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text  = if (modeFlag) stringResource(R.string.switch_to_local) else stringResource(R.string.switch_to_streaming),
                                                color = Color.White, fontSize = 14.sp
                                            )
                                            Text(
                                                text  = if (modeFlag)
                                                    stringResource(R.string.mode_hint_to_local)
                                                else
                                                    stringResource(R.string.mode_hint_to_streaming),
                                                color    = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                },
                                onClick = { modeFlag = !modeFlag; showMenu = false }
                            )

                            HorizontalDivider(
                                color    = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.QueueMusic, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.view_queue), color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Share, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.share), color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = { showMenu = false }
                            )

                            HorizontalDivider(
                                color    = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Album, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.go_to_album), color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                        Text(stringResource(R.string.go_to_artist), color = Color.White, fontSize = 14.sp)
                                    }
                                },
                                onClick = { showMenu = false }
                            )
                        }
                    }

                    HorizontalDivider(
                        color     = Color.White.copy(alpha = 0.15f),
                        thickness = 1.dp,
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Info, "Info",
                            tint     = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier            = Modifier.padding(end = 8.dp)
                ) { }
            }
        }
    }
}



@Preview(
    name            = "Player – Reproduciendo",
    showBackground  = true,
    backgroundColor = 0xFF051E26,
    showSystemUi    = true,
    widthDp         = 390,
    heightDp        = 844
)
@Composable
private fun PreviewPlayerPlaying() {
    musicPlayerScreenStateless(
        songTitle       = "Never Gonna Give You Up",
        songArtist      = "Rick Astley",
        artUri          = null,
        isPlaying       = true,
        isLoading       = false,
        isLiked         = false,
        isStreamingMode = true,
        shuffleEnabled  = false,
        repeatMode      = Player.REPEAT_MODE_OFF,
        canSkipNext     = true,
        canSkipPrevious = true,
        currentPosition = 67_000L,
        duration        = 213_000L,
        accentColor     = previewAccentColor,
        contextPrefix   = "Album",
        contextLabel    = "Whenever You Need Somebody",
        contextIcon     = Icons.Default.Album,
        onBackClick     = {}
    )
}

@Preview(
    name            = "Player – Cargando",
    showBackground  = true,
    backgroundColor = 0xFF051E26,
    showSystemUi    = true,
    widthDp         = 390,
    heightDp        = 844
)
@Composable
private fun PreviewPlayerLoading() {
    musicPlayerScreenStateless(
        songTitle       = "Cargando canción...",
        songArtist      = "Artista Desconocido",
        artUri          = null,
        isPlaying       = false,
        isLoading       = true,
        isLiked         = false,
        isStreamingMode = false,
        shuffleEnabled  = true,
        repeatMode      = Player.REPEAT_MODE_ONE,
        canSkipNext     = false,
        canSkipPrevious = false,
        currentPosition = 0L,
        duration        = 0L,
        accentColor     = previewAccentColor,
        contextPrefix   = "Artista",
        contextLabel    = "Artista Demo",
        contextIcon     = Icons.Default.Person,
        onBackClick     = {}
    )
}

@Preview(
    name            = "Player – Pausado / Liked",
    showBackground  = true,
    backgroundColor = 0xFF051E26,
    showSystemUi    = true,
    widthDp         = 390,
    heightDp        = 844
)
@Composable
private fun PreviewPlayerPaused() {
    musicPlayerScreenStateless(
        songTitle       = "Blinding Lights",
        songArtist      = "The Weeknd",
        artUri          = null,
        isPlaying       = false,
        isLoading       = false,
        isLiked         = true,
        isStreamingMode = true,
        shuffleEnabled  = true,
        repeatMode      = Player.REPEAT_MODE_ALL,
        canSkipNext     = true,
        canSkipPrevious = true,
        currentPosition = 120_000L,
        duration        = 200_000L,
        accentColor     = previewAccentColor,
        contextPrefix   = "Playlist",
        contextLabel    = "Mis Favoritos",
        contextIcon     = Icons.Default.QueueMusic,
        onBackClick     = {}
    )
}























