/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.search

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.AppBarHeight
import com.betsy.music.constants.SearchFilterHeight
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.MediaMetadata
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.ui.component.EmptyPlaceholder
import com.betsy.music.ui.component.NavigationTitle
import com.betsy.music.ui.component.shimmer.ListItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.tooling.preview.Preview


enum class LocalResultFilter { ALL, SONGS }


private data class LocalResultSong(
    val id          : Long,
    val title       : String,
    val artist      : String,
    val album       : String,
    val duration    : Long,
    val uri         : Uri,
    val albumArtUri : Uri?,
)

private fun LocalResultSong.toPlayableMediaItem(): androidx.media3.common.MediaItem {
    val mediaId = "device_$id"
    val meta = MediaMetadata(
        id           = mediaId,
        title        = title,
        artists      = if (artist.isNotBlank())
            listOf(MediaMetadata.Artist(id = null, name = artist)) else emptyList(),
        album        = album.takeIf { it.isNotBlank() }
            ?.let { MediaMetadata.Album(id = "", title = it) },
        duration     = (duration / 1000).toInt(),
        thumbnailUrl = albumArtUri?.toString(),
    )
    val extras = android.os.Bundle().apply {
        putString("mediaId", mediaId)
        putBoolean("isLocal", true)
    }
    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(uri)
        .setCustomCacheKey(mediaId)
        .setRequestMetadata(
            androidx.media3.common.MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri).setExtras(extras).build()
        )
        .setTag(meta)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title).setArtist(artist).setAlbumTitle(album)
                .setArtworkUri(albumArtUri).build()
        ).build()
}








@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalSearchResult(
    navController: NavController,
) {
    val context          = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying     by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    
    val rawQuery = navController.currentBackStackEntry?.arguments?.getString("query") ?: ""
    val query = remember(rawQuery) {
        if (rawQuery.contains("%")) {
            runCatching { java.net.URLDecoder.decode(rawQuery, "UTF-8") }.getOrDefault(rawQuery)
        } else rawQuery
    }

    
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val hasPermission = remember {
        context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED
    }

    
    var filter   by remember { mutableStateOf(LocalResultFilter.ALL) }
    var results  by remember { mutableStateOf<List<LocalResultSong>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    
    LaunchedEffect(query, hasPermission) {
        isLoading = true
        results = if (!hasPermission || query.length < 1) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                val list       = mutableListOf<LocalResultSong>()
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                )
                
                
                
                
                val normalized = query.trim()
                    .replace(Regex("[''`´'`\\-]"), " ")
                    .replace(Regex("[^\\w\\s]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim().lowercase()
                val words = normalized.split(" ").filter { it.isNotEmpty() }
                if (words.isEmpty()) return@withContext emptyList()
                val firstWord = words.first().replace("'", "''")

                val sel = "${MediaStore.Audio.Media.IS_MUSIC} != 0 " +
                        "AND ${MediaStore.Audio.Media.DURATION} > 30000 " +
                        "AND (${MediaStore.Audio.Media.TITLE} LIKE '%$firstWord%' " +
                        "OR ${MediaStore.Audio.Media.ARTIST} LIKE '%$firstWord%' " +
                        "OR ${MediaStore.Audio.Media.ALBUM} LIKE '%$firstWord%')"

                context.contentResolver.query(
                    collection, projection, sel, null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )?.use { cursor ->
                    val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    while (cursor.moveToNext()) {
                        val sid     = cursor.getLong(idCol)
                        val albumId = cursor.getLong(albumIdCol)
                        list.add(LocalResultSong(
                            id          = sid,
                            title       = cursor.getString(titleCol)  ?: context.getString(R.string.unknown),
                            artist      = cursor.getString(artistCol) ?: context.getString(R.string.unknown_artist),
                            album       = cursor.getString(albumCol)  ?: context.getString(R.string.unknown_album),
                            duration    = cursor.getLong(durCol),
                            uri         = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, sid),
                            albumArtUri = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"), albumId),
                        ))
                    }
                }

                
                
                if (words.size > 1) {
                    val rest = words.drop(1)
                    list.filter { song ->
                        val haystack = "${song.title} ${song.artist} ${song.album}"
                            .lowercase()
                            .replace(Regex("[''`´'`\\-]"), " ")
                            .replace(Regex("[^\\w\\s]"), " ")
                        rest.all { word -> haystack.contains(word) }
                    }
                } else {
                    list
                }
            }
        }
        isLoading = false
    }

    val lazyListState = rememberLazyListState()

    
    val filteredResults = remember(results, filter) {
        when (filter) {
            LocalResultFilter.ALL   -> results
            LocalResultFilter.SONGS -> results  
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .add(WindowInsets(top = SearchFilterHeight))
                .add(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .asPaddingValues(),
        ) {

            if (!isLoading && filteredResults.isNotEmpty()) {
                item {
                    NavigationTitle(
                        stringResource(R.string.filter_songs) +
                                " (${filteredResults.size})"
                    )
                }
                items(items = filteredResults, key = { "loc_${it.id}" }) { song ->
                    val isActive = "device_${song.id}" == mediaMetadata?.id
                    LocalResultSongRow(
                        song        = song,
                        isActive    = isActive,
                        isPlaying   = isActive && isPlaying,
                        accentColor = accentColor,
                        onClick     = {
                            if (isActive) playerConnection.player.togglePlayPause()
                            else {
                                val idx   = filteredResults.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                val items = filteredResults.map { it.toPlayableMediaItem() }
                                playerConnection.playQueue(
                                    ListQueue(title = query, items = items, startIndex = idx)
                                )
                            }
                        }
                    )
                }
            }

            if (!isLoading && filteredResults.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }

            if (isLoading) {
                item {
                    ShimmerHost { repeat(8) { ListItemPlaceHolder() } }
                }
            }

            if (!hasPermission) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.storage_permission_local),
                    )
                }
            }
        }

        
        SearchChipsRow(
            chips = listOf(
                LocalResultFilter.ALL   to stringResource(R.string.filter_all),
                LocalResultFilter.SONGS to stringResource(R.string.filter_songs),
            ),
            currentValue  = filter,
            onValueUpdate = { filter = it },
            accentColor   = accentColor,
            modifier = Modifier
                .background(Color.Transparent)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(top = AppBarHeight))
                )
                .fillMaxWidth(),
        )
    }
}




@Composable
private fun <E> SearchChipsRow(
    chips        : List<Pair<E, String>>,
    currentValue : E,
    onValueUpdate: (E) -> Unit,
    accentColor  : Color,
    modifier     : Modifier = Modifier,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        chips.forEach { (value, label) ->
            val isSelected = currentValue == value
            Surface(
                onClick  = { onValueUpdate(value) },
                shape    = RoundedCornerShape(50),
                color    = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                border   = BorderStroke(
                    1.dp,
                    if (isSelected) accentColor.copy(alpha = 0.7f) else adaptiveColor.copy(alpha = 0.15f),
                ),
                modifier = Modifier.height(34.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(
                        text       = label,
                        color      = if (isSelected) accentColor else adaptiveColor.copy(alpha = 0.55f),
                        fontSize   = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}




@Composable
private fun LocalResultEqualizer(
    color   : Color,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loc_eq_transition")
    val bars = List(barCount) { i ->
        val duration = 480 + i * 140
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue  = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(durationMillis = duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "loc_eq_bar_$i",
        )
    }

    Row(
        modifier              = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment     = Alignment.Bottom,
    ) {
        bars.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(anim.value)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}


@Composable
private fun LocalResultSongRow(
    song        : LocalResultSong,
    isActive    : Boolean,
    isPlaying   : Boolean,
    accentColor : Color,
    onClick     : () -> Unit,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    val dur = remember(song.duration) {
        val s = (song.duration / 1000).toInt()
        "%d:%02d".format(s / 60, s % 60)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.3f) else Color.Transparent)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model              = song.albumArtUri,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.MusicNote, null,
                    tint     = accentColor,
                    modifier = Modifier.size(26.dp),
                )
            }

            
            if (isActive && isPlaying) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LocalResultEqualizer(color = accentColor)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = song.title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = if (isActive) accentColor else adaptiveColor,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = song.artist,
                fontSize = 12.sp,
                color    = adaptiveColor.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(dur, fontSize = 11.sp, color = adaptiveColor.copy(alpha = 0.4f))
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector        = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
            contentDescription = null,
            tint               = accentColor.copy(alpha = if (isActive) 1f else 0.4f),
            modifier           = Modifier.size(18.dp),
        )
    }
}




@Preview(
    name = "Búsqueda local",
    showBackground = true,
    showSystemUi = true,
    widthDp = 411,
    heightDp = 891
)
@Composable
private fun LocalSearchResultPreview() {

    val accentColor = Color(0xFF3B9CAC)

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 112.dp,
                        bottom = 24.dp
                    )
                ) {

                    
                    
                    

                    item {

                        Text(
                            text = "Canciones (6)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                        )
                    }


                    
                    
                    

                    item {
                        PreviewLocalSong(
                            title = "No Hay Nadie Más",
                            artist = "Sebastián Yatra",
                            duration = "3:59",
                            accentColor = accentColor
                        )
                    }

                    item {
                        PreviewLocalSong(
                            title = "Titanic",
                            artist = "Deivid Gera",
                            duration = "3:21",
                            accentColor = accentColor,
                            isPlaying = true
                        )
                    }

                    item {
                        PreviewLocalSong(
                            title = "Saturn",
                            artist = "SZA",
                            duration = "3:06",
                            accentColor = accentColor
                        )
                    }

                    item {
                        PreviewLocalSong(
                            title = "Die With A Smile",
                            artist = "Lady Gaga, Bruno Mars",
                            duration = "4:11",
                            accentColor = accentColor
                        )
                    }

                    item {
                        PreviewLocalSong(
                            title = "Blinding Lights",
                            artist = "The Weeknd",
                            duration = "3:20",
                            accentColor = accentColor
                        )
                    }

                    item {
                        PreviewLocalSong(
                            title = "La Bachata",
                            artist = "Manuel Turizo",
                            duration = "2:48",
                            accentColor = accentColor
                        )
                    }
                }


                
                
                

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.background
                                .copy(alpha = 0.96f)
                        )
                        .padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    PreviewSearchChip(
                        text = "Todos",
                        selected = true,
                        accentColor = accentColor
                    )

                    PreviewSearchChip(
                        text = "Canciones",
                        selected = false,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSearchChip(
    text: String,
    selected: Boolean,
    accentColor: Color
) {

    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) {
            accentColor.copy(alpha = 0.18f)
        } else {
            Color.Transparent
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                accentColor.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurface
                    .copy(alpha = 0.15f)
            }
        ),
        modifier = Modifier.height(34.dp)
    ) {

        Box(
            modifier = Modifier.padding(
                horizontal = 16.dp
            ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                color = if (selected) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                        .copy(alpha = 0.55f)
                },
                fontSize = 13.sp,
                fontWeight = if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}

@Composable
private fun PreviewLocalSong(
    title: String,
    artist: String,
    duration: String,
    accentColor: Color,
    isPlaying: Boolean = false
) {

    val adaptiveColor =
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPlaying) {
                    accentColor.copy(alpha = 0.30f)
                } else {
                    Color.Transparent
                }
            )
            .border(
                width = if (isPlaying) 1.5.dp else 0.dp,
                color = if (isPlaying) {
                    accentColor
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        
        
        

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    accentColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )

            if (isPlaying) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.45f)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(3.dp),
                        verticalAlignment =
                            Alignment.Bottom
                    ) {

                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(10.dp)
                                .background(accentColor)
                        )

                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(accentColor)
                        )

                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(7.dp)
                                .background(accentColor)
                        )
                    }
                }
            }
        }


        Spacer(
            Modifier.width(12.dp)
        )


        
        
        

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPlaying) {
                    accentColor
                } else {
                    adaptiveColor
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = artist,
                fontSize = 12.sp,
                color = adaptiveColor.copy(
                    alpha = 0.55f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }


        Spacer(
            Modifier.width(8.dp)
        )


        
        
        

        Text(
            text = duration,
            fontSize = 11.sp,
            color = adaptiveColor.copy(
                alpha = 0.4f
            )
        )


        Spacer(
            Modifier.width(6.dp)
        )


        
        
        

        Icon(
            imageVector = if (isPlaying) {
                Icons.Default.MusicNote
            } else {
                Icons.Default.PlayArrow
            },
            contentDescription = null,
            tint = accentColor.copy(
                alpha = if (isPlaying) 1f else 0.4f
            ),
            modifier = Modifier.size(18.dp)
        )
    }
}