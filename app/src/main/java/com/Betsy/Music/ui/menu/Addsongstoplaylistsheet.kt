/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.menu

import androidx.compose.ui.res.stringResource
import com.betsy.music.R
import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.betsy.audius.compat.AudiusApi
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.utils.toMediaMetadata as iaToMediaMetadata
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.Playlist
import com.betsy.music.db.entities.SongEntity
import com.betsy.music.models.MediaMetadata
import com.betsy.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private enum class SongSource { STREAMING, LOCAL }


private data class LocalDeviceSong(
    val id          : Long,
    val title       : String,
    val artist      : String,
    val album       : String,
    val duration    : Long,       
    val uri         : Uri,
    val albumArtUri : Uri?,
)

   
                                                
  
                                                            
                                                                         
                                                                                       
  
                                                                                  
                                                          
  
                                                
                                                   
   
@Composable
fun AddSongsToPlaylistSheet(
    playlistId : String,
    onDismiss  : () -> Unit,
) {
    val context          = LocalContext.current
    val database         = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope   = rememberCoroutineScope()

    
    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    LaunchedEffect(playlistId) {
        database.playlist(playlistId).collect { playlist = it }
    }

    
    
    val isMixedPlaylist = playlist?.playlist?.isMixed == true
    val (appStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)
    var chosenSource by remember { mutableStateOf<SongSource?>(null) }
    val selectedSource = when {
        isMixedPlaylist -> chosenSource
            ?: if (appStreamingMode) SongSource.STREAMING else SongSource.LOCAL
        playlist?.playlist?.isLocalMode == true -> SongSource.LOCAL
        else -> SongSource.STREAMING
    }

    
    var playlistSongIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(playlistId) {
        database.playlistSongs(playlistId).collect { songs ->
            playlistSongIds = songs.map { it.map.songId }.toSet()
        }
    }

    
    var query            by remember { mutableStateOf("") }
    var streamingResults by remember { mutableStateOf<List<MediaMetadata>>(emptyList()) }
    var localResults     by remember { mutableStateOf<List<LocalDeviceSong>>(emptyList()) }
    var isLoading        by remember { mutableStateOf(false) }
    var searchJob        by remember { mutableStateOf<Job?>(null) }

    
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE
    val hasAudioPermission = remember {
        context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED
    }

    
    suspend fun searchLocal(q: String): List<LocalDeviceSong> = withContext(Dispatchers.IO) {
        if (!hasAudioPermission || q.length < 2) return@withContext emptyList()
        val list       = mutableListOf<LocalDeviceSong>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        
        
        val words = q.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return@withContext emptyList()
        val wordCond = words.joinToString(" AND ") {
            "(LOWER(REPLACE(${MediaStore.Audio.Media.TITLE}, '''', '')) LIKE ? " +
                    "OR LOWER(REPLACE(${MediaStore.Audio.Media.ARTIST}, '''', '')) LIKE ? " +
                    "OR LOWER(REPLACE(${MediaStore.Audio.Media.ALBUM}, '''', '')) LIKE ?)"
        }
        val sel  = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000 AND $wordCond"
        val args = words.flatMap { listOf("%$it%", "%$it%", "%$it%") }.toTypedArray()
        context.contentResolver.query(
            collection, projection, sel, args,
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
                list.add(LocalDeviceSong(
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
        list
    }

    
    suspend fun searchStreaming(q: String): List<MediaMetadata> = withContext(Dispatchers.IO) {
        if (q.length < 2) return@withContext emptyList()
        coroutineScope {
            val audiusJob = async {
                runCatching {
                    AudiusApi.search(q, AudiusApi.SearchFilter.FILTER_SONG).getOrNull()
                        ?.items
                        ?.filterIsInstance<com.betsy.audius.compat.models.SongItem>()
                        ?.map { item ->
                            MediaMetadata(
                                id           = item.id,
                                title        = item.title,
                                artists      = item.artists.map {
                                    MediaMetadata.Artist(id = it.id, name = it.name)
                                },
                                album        = item.album?.let {
                                    MediaMetadata.Album(id = it.id ?: "", title = it.name)
                                },
                                duration     = item.duration ?: 0,
                                thumbnailUrl = item.thumbnail,
                            )
                        } ?: emptyList()
                }.getOrDefault(emptyList())
            }
            val iaJob = async {
                runCatching {
                    val page = InternetArchive.client.search(q, page = 1, rows = 5).getOrNull()
                    page?.items.orEmpty()
                        .map { item ->
                            async { InternetArchive.client.details(item.identifier).getOrNull()?.tracks.orEmpty() }
                        }
                        .awaitAll()
                        .flatten()
                        .take(40)
                        .map { it.iaToMediaMetadata() }
                }.getOrDefault(emptyList())
            }
            (audiusJob.await() + iaJob.await()).distinctBy { it.id }
        }
    }

    
    LaunchedEffect(query, selectedSource) {
        searchJob?.cancel()
        if (query.length < 2) {
            streamingResults = emptyList()
            localResults     = emptyList()
            isLoading        = false
            return@LaunchedEffect
        }
        searchJob = coroutineScope.launch {
            delay(400)
            isLoading = true
            when (selectedSource) {
                SongSource.STREAMING -> {
                    streamingResults = searchStreaming(query)
                    localResults     = emptyList()
                }
                SongSource.LOCAL -> {
                    localResults     = searchLocal(query)
                    streamingResults = emptyList()
                }
            }
            isLoading = false
        }
    }

    
    fun addStreamingSong(meta: MediaMetadata) {
        val pl = playlist ?: return
        coroutineScope.launch(Dispatchers.IO) {
            database.transaction {
                
                insert(meta)
                addSongToPlaylist(pl, listOf(meta.id))
            }
        }
    }

    
    fun addLocalSong(song: LocalDeviceSong) {
        val pl      = playlist ?: return
        val mediaId = "device_${song.id}"
        coroutineScope.launch(Dispatchers.IO) {
            database.transaction {
                
                insert(
                    SongEntity(
                        id           = mediaId,
                        title        = song.title,
                        duration     = (song.duration / 1000).toInt(),
                        thumbnailUrl = song.albumArtUri?.toString(),
                        albumName    = song.album.takeIf { it.isNotBlank() },
                    )
                )
                addSongToPlaylist(pl, listOf(mediaId))
            }
        }
    }

    
    fun removeSong(songId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val maps = database.playlistSongs(playlistId).first()
            maps.filter { it.map.songId == songId }
                .forEach { database.transaction { delete(it.map) } }
        }
    }

    
    
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .background(Color(0xFF111B1D))
    ) {

        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = stringResource(R.string.edit_playlist),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                modifier   = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint               = Color.White,
                )
            }
        }

        
        if (isMixedPlaylist) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SongSource.values().forEach { source ->
                    val isSelected = selectedSource == source
                    FilterChip(
                        selected = isSelected,
                        onClick  = { chosenSource = source },
                        label    = {
                            Text(
                                text  = if (source == SongSource.STREAMING) stringResource(R.string.lib_mode_streaming)
                                else stringResource(R.string.on_my_device),
                                color = if (isSelected) Color.Black else Color.White,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor  = accentColor,
                            selectedLabelColor      = Color.Black,
                            containerColor          = Color.White.copy(alpha = 0.08f),
                            labelColor              = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = isSelected,
                            borderColor         = Color.White.copy(alpha = 0.15f),
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }
        }

        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = null,
                tint               = accentColor,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value           = query,
                onValueChange   = { query = it },
                singleLine      = true,
                textStyle       = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush     = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox   = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text     = if (selectedSource == SongSource.STREAMING)
                                stringResource(R.string.search_hint_streaming)
                            else
                                stringResource(R.string.search_hint_device),
                            color    = Color.White.copy(alpha = 0.35f),
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier    = Modifier.size(16.dp),
                    color       = accentColor,
                    strokeWidth = 2.dp,
                )
            }
        }

        
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            
            if (selectedSource == SongSource.STREAMING) {
                items(items = streamingResults, key = { "st_${it.id}" }) { meta ->
                    val isInPlaylist = meta.id in playlistSongIds
                    StreamingSongRow(
                        meta         = meta,
                        isInPlaylist = isInPlaylist,
                        accentColor  = accentColor,
                        onToggle     = {
                            if (isInPlaylist) removeSong(meta.id)
                            else addStreamingSong(meta)
                        },
                    )
                }
            }

            
            if (selectedSource == SongSource.LOCAL) {
                if (!hasAudioPermission) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text     = stringResource(R.string.storage_permission_local),
                                color    = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    items(items = localResults, key = { "loc_${it.id}" }) { song ->
                        val mediaId      = "device_${song.id}"
                        val isInPlaylist = mediaId in playlistSongIds
                        LocalSongRow(
                            song         = song,
                            isInPlaylist = isInPlaylist,
                            accentColor  = accentColor,
                            onToggle     = {
                                if (isInPlaylist) removeSong(mediaId)
                                else addLocalSong(song)
                            },
                        )
                    }
                }
            }

            
            if (query.length >= 2 && !isLoading &&
                streamingResults.isEmpty() && localResults.isEmpty()
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text     = stringResource(R.string.no_songs_found),
                            color    = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            
            if (query.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text     = if (selectedSource == SongSource.STREAMING)
                                stringResource(R.string.type_to_search_streaming)
                            else
                                stringResource(R.string.type_to_search_phone),
                            color    = Color.White.copy(alpha = 0.3f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun StreamingSongRow(
    meta         : MediaMetadata,
    isInPlaylist : Boolean,
    accentColor  : Color,
    onToggle     : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!meta.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model              = meta.thumbnailUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = accentColor)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = meta.title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = meta.artists.joinToString(", ") { it.name },
                fontSize = 12.sp,
                color    = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isInPlaylist) accentColor else accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = if (isInPlaylist) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint               = if (isInPlaylist) Color.Black else accentColor,
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}


@Composable
private fun LocalSongRow(
    song         : LocalDeviceSong,
    isInPlaylist : Boolean,
    accentColor  : Color,
    onToggle     : () -> Unit,
) {
    val dur = remember(song.duration) {
        val s = (song.duration / 1000).toInt()
        "%d:%02d".format(s / 60, s % 60)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.12f)),
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
                Icon(Icons.Default.MusicNote, null, tint = accentColor)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = song.title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = song.artist,
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text     = dur,
                    fontSize = 11.sp,
                    color    = Color.White.copy(alpha = 0.3f),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isInPlaylist) accentColor else accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = if (isInPlaylist) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint               = if (isInPlaylist) Color.Black else accentColor,
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}