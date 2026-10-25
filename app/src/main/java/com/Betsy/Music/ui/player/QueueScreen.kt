/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import coil.compose.AsyncImage
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.extensions.metadata
import com.betsy.music.extensions.move
import com.betsy.music.utils.makeTimeString
import com.betsy.music.utils.rememberPreference
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

   
                                                                               
                                                                                    
                                                                   
                                                                               
                                                                
   
@Composable
fun QueueScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentIndex by playerConnection.currentWindowIndex.collectAsState()

    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = Color(accentColorInt.toLong() or 0xFF000000L)

    val listState = rememberLazyListState()

    
    
    val items = remember {
        mutableStateListOf<Timeline.Window>().apply { addAll(queueWindows) }
    }

    
    LaunchedEffect(queueWindows) {
        items.clear()
        items.addAll(queueWindows)
    }

    
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragTo by remember { mutableStateOf(0) }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
    ) { from, to ->
        if (from.index in items.indices && to.index in items.indices) {
            if (dragFrom == null) dragFrom = from.index
            dragTo = to.index
            items.move(from.index, to.index)
        }
    }

    
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            val from = dragFrom
            if (from != null) {
                val to = dragTo
                dragFrom = null
                if (from != to) {
                    val player = playerConnection.player
                    if (!player.shuffleModeEnabled) {
                        player.moveMediaItem(from, to)
                    } else {
                        
                        player.setShuffleOrder(
                            DefaultShuffleOrder(
                                queueWindows
                                    .map { it.firstPeriodIndex }
                                    .toMutableList()
                                    .move(from, to)
                                    .toIntArray(),
                                System.currentTimeMillis(),
                            )
                        )
                    }
                }
            }
        }
    }

    
    LaunchedEffect(queueWindows.size) {
        if (currentIndex in queueWindows.indices) {
            listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    
    val currentUid = queueWindows.getOrNull(currentIndex)?.uid

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A2F3D), Color(0xFF06141C))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Cola de reproducción",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (queueWindows.size == 1) "1 canción" else "${queueWindows.size} canciones",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            if (queueWindows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "La cola está vacía",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = items,
                        
                        key = { _, window -> window.uid.hashCode() }
                    ) { _, window ->
                        ReorderableItem(
                            state = reorderableState,
                            key = window.uid.hashCode(),
                        ) { isDragging ->
                            val song = window.mediaItem.metadata ?: return@ReorderableItem
                            val isCurrent = window.uid == currentUid

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isDragging) 1.dp else 0.dp,
                                        color = if (isDragging) accentColor.copy(alpha = 0.6f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isDragging -> accentColor.copy(alpha = 0.30f)
                                            isCurrent -> accentColor.copy(alpha = 0.18f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        if (!isCurrent) {
                                            playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                        }
                                        playerConnection.player.playWhenReady = true
                                    }
                                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (song.thumbnailUrl != null) {
                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.MusicNote, null,
                                            tint = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.VolumeUp, null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) accentColor else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artists.joinToString { it.name },
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                
                                if (song.duration > 0) {
                                    Text(
                                        text = makeTimeString(song.duration * 1000L),
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }

                                
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier
                                        .size(40.dp)
                                        .draggableHandle()
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.drag_handle),
                                        contentDescription = "Reordenar",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}