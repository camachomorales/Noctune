/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.music.LocalDatabase
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.PauseSearchHistoryKey
import com.betsy.music.db.entities.SearchHistory
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import com.betsy.music.utils.makeTimeString
import com.betsy.music.utils.rememberPreference
import com.betsy.music.utils.rememberSearchOwnerId
import com.betsy.music.viewmodels.LocalDeviceSong
import com.betsy.music.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.flow.drop

   
                                                               
                                                                    
                                                  
                                                         
  
                                                                          
                                                                               
  
                                                                                
                                                                                   
                                                                                    
   
@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    pureBlack: Boolean = false,
    onQueryChange: ((TextFieldValue) -> Unit)? = null,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val ownerId = rememberSearchOwnerId()

    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    val state by viewModel.viewState.collectAsState()
    val typed = query.trim()

    LaunchedEffect(query, ownerId) {
        viewModel.query.value = query
        viewModel.ownerId.value = ownerId
    }

    val lazyListState = rememberLazyListState()
    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect { keyboardController?.hide() }
    }

    
    val openFullSearch: (String) -> Unit = { text ->
        val q = text.trim()
        if (q.isNotEmpty()) {
            onQueryChange?.invoke(TextFieldValue(text = q, selection = TextRange(q.length)))
            keyboardController?.hide()
            if (context.dataStore[PauseSearchHistoryKey] != true) {
                database.query { insert(SearchHistory(query = q, ownerId = ownerId)) }
            }
            navController.navigate("local_search/${Uri.encode(q)}")
            onDismiss()
        }
    }

    val fillTextField: ((String) -> Unit)? =
        onQueryChange?.let { change ->
            { text: String -> change(TextFieldValue(text = text, selection = TextRange(text.length))) }
        }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {

        
        if (state.history.isNotEmpty()) {
            item(key = "history_header") {
                SectionTitle(stringResource(R.string.SearchHistory), accentColor)
            }
            items(items = state.history, key = { "history_${it.id}" }) { history ->
                SuggestionItem(
                    query = history.query,
                    online = false,
                    onClick = { openFullSearch(history.query) },
                    onDelete = { database.query { delete(history) } },
                    onFillTextField = { fillTextField?.invoke(history.query) },
                    showFillButton = fillTextField != null,
                )
            }
        }

        
        if (typed.isNotEmpty() && state.history.none { it.query == typed }) {
            item(key = "suggestions_header") {
                SectionTitle(stringResource(R.string.Sujestions), accentColor)
            }
            item(key = "typed_query") {
                SuggestionItem(
                    query = typed,
                    online = true,
                    onClick = { openFullSearch(typed) },
                    onFillTextField = {},
                    showFillButton = false,
                )
            }
        }

        
        if (state.songs.isNotEmpty()) {
            item(key = "songs_header") {
                SectionTitle(stringResource(R.string.filter_songs), accentColor)
            }
            items(items = state.songs, key = { "local_song_${it.id}" }) { song ->
                LocalSongSuggestionItem(
                    song = song,
                    onClick = { openFullSearch(song.title) },
                    onFillTextField = fillTextField?.let { fill -> { fill(song.title) } },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, accentColor: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = accentColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LocalSongSuggestionItem(
    song: LocalDeviceSong,
    onClick: () -> Unit,
    onFillTextField: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    val subtitle = listOf(song.artist, makeTimeString(song.durationMs))
        .filter { it.isNotBlank() }
        .joinToString(" • ")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = adaptiveColor.copy(alpha = 0.08f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(adaptiveColor.copy(alpha = 0.10f)),
            ) {
                
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = adaptiveColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp),
                )
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = adaptiveColor,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = adaptiveColor.copy(alpha = 0.6f),
                    )
                }
            }

            if (onFillTextField != null) {
                IconButton(
                    onClick = onFillTextField,
                    modifier = Modifier.alpha(0.7f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_top_left),
                        contentDescription = stringResource(R.string.fill_search),
                        tint = adaptiveColor,
                    )
                }
            }
        }
    }
}
