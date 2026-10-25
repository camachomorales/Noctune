/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.viewmodels

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betsy.music.db.entities.SearchHistory
import com.betsy.music.db.musicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.text.Normalizer
import javax.inject.Inject

                                                      
data class LocalDeviceSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUri: Uri?,
)

                                                          
data class LocalSearchViewState(
    val history: List<SearchHistory> = emptyList(),
    val songs: List<LocalDeviceSong> = emptyList(),
)

private val diacriticsRegex = Regex("\\p{Mn}+")
private val separatorsRegex = Regex("[^\\p{L}\\p{Nd}]+")

   
                                          
                                                                      
                                           
   
private fun fold(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(diacriticsRegex, "")
        .lowercase()
        .replace(separatorsRegex, " ")
        .trim()

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LocalSearchViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    database: musicDatabase,
) : ViewModel() {

    val query = MutableStateFlow("")
    val ownerId = MutableStateFlow("")

    private val trimmedQuery = query.map { it.trim() }.distinctUntilChanged()

    
    
    private val historyFlow =
        combine(trimmedQuery, ownerId) { q, owner -> q to owner }
            .flatMapLatest { (q, owner) ->
                database.searchHistory(q, owner).map { it.take(MAX_HISTORY) }
            }

    
    private val songsFlow =
        trimmedQuery
            .debounce(120)
            .mapLatest { q ->
                if (q.isEmpty()) emptyList()
                else withContext(Dispatchers.IO) { searchDeviceSongs(q) }
            }

    val viewState: StateFlow<LocalSearchViewState> =
        combine(historyFlow, songsFlow) { history, songs ->
            LocalSearchViewState(history = history, songs = songs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalSearchViewState())

    

    private class Entry(val song: LocalDeviceSong, val title: String, val haystack: String)

    @Volatile private var cache: List<Entry> = emptyList()
    @Volatile private var cacheAt = 0L

    private fun hasAudioPermission(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    @Synchronized
    private fun deviceEntries(): List<Entry> {
        if (!hasAudioPermission()) return emptyList()
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && now - cacheAt < CACHE_MS) return cache

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
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
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"

        val list = mutableListOf<Entry>()
        runCatching {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    val title = c.getString(titleCol).orEmpty()
                    val artist = c.getString(artistCol).orEmpty().takeUnless { it == "<unknown>" }.orEmpty()
                    val album = c.getString(albumCol).orEmpty().takeUnless { it == "<unknown>" }.orEmpty()
                    val song = LocalDeviceSong(
                        id = c.getLong(idCol),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = c.getLong(durCol),
                        albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            c.getLong(albumIdCol),
                        ),
                    )
                    list += Entry(song, fold(title), fold("$title $artist $album"))
                }
            }
        }
        cache = list
        cacheAt = now
        return list
    }

    private fun searchDeviceSongs(rawQuery: String): List<LocalDeviceSong> {
        val folded = fold(rawQuery)
        if (folded.isEmpty()) return emptyList()
        val words = folded.split(' ').filter { it.isNotEmpty() }
        return deviceEntries()
            .asSequence()
            .filter { entry -> words.all { entry.haystack.contains(it) } }
            .sortedWith(
                compareBy<Entry>(
                    { if (it.title.startsWith(folded)) 0 else if (it.title.contains(folded)) 1 else 2 },
                    { it.title },
                ),
            )
            .take(MAX_SONGS)
            .map { it.song }
            .toList()
    }

    companion object {
        const val MAX_HISTORY = 5
        const val MAX_SONGS = 8
        private const val CACHE_MS = 30_000L
    }
}
