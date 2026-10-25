/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.player

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.utils.rememberPreference

@Composable
fun MiniPlayer(
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying     by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipNext   by playerConnection.canSkipNext.collectAsState()

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    
    LaunchedEffect(playbackState) {
        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
            while (true) {
                kotlinx.coroutines.delay(500)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration.coerceAtLeast(0L)
            }
        }
    }

    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    
    
    
    
    AnimatedVisibility(
        visible  = mediaMetadata != null,
        enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        
        val title    = mediaMetadata?.title    ?: return@AnimatedVisibility
        val artist   = mediaMetadata?.artists?.joinToString { it.name } ?: ""
        val thumbUrl = mediaMetadata?.thumbnailUrl

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(adaptiveColor.copy(alpha = 0.1f))
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onNavigateToPlayer() }
        ) {
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
            ) {
                drawLine(
                    color       = adaptiveColor.copy(alpha = 0.15f),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = size.height,
                    cap         = StrokeCap.Round
                )
                drawLine(
                    color       = accentColor,
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width * progress, 0f),
                    strokeWidth = size.height,
                    cap         = StrokeCap.Round
                )
            }

            Row(
                modifier          = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                Box(
                    modifier         = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A4A5A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model              = thumbUrl,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint               = accentColor,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = title,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = adaptiveColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (artist.isNotEmpty()) {
                        Text(
                            text     = artist,
                            fontSize = 12.sp,
                            color    = adaptiveColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                
                IconButton(
                    onClick  = { playerConnection.player.togglePlayPause() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.pause_action) else stringResource(R.string.play),
                        tint               = adaptiveColor,
                        modifier           = Modifier.size(26.dp)
                    )
                }

                
                IconButton(
                    onClick  = { playerConnection.seekToNext() },
                    modifier = Modifier.size(40.dp),
                    enabled  = canSkipNext
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.next_action),
                        tint               = if (canSkipNext) adaptiveColor else adaptiveColor.copy(alpha = 0.3f),
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}












