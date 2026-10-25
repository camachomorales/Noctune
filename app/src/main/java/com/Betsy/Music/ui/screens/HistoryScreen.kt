/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import com.betsy.music.ui.theme.rememberAdaptivePanelColor
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.HistorySource
import com.betsy.music.db.entities.EventWithSong
import com.betsy.music.extensions.metadata
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.ListQueue
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.IconButton
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.PlayingIndicatorBox
import com.betsy.music.ui.component.StreamingListItem
import com.betsy.music.ui.menu.SelectionMediaMetadataMenu
import com.betsy.music.ui.menu.SongMenu
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.DateAgo
import com.betsy.music.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var pinnedHeight by remember { mutableStateOf(0.dp) }

    var selection by remember { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }
    if (isSearching) {
        BackHandler { isSearching = false; query = TextFieldValue() }
    } else if (selection) {
        BackHandler { selection = false }
    }

    val historySource by viewModel.historySource.collectAsState()
    val events by viewModel.events.collectAsState()
    val remoteEvents by viewModel.remoteEvents.collectAsState()

    fun dateAgoToString(dateAgo: DateAgo): String = when (dateAgo) {
        DateAgo.Today     -> context.getString(R.string.today)
        DateAgo.Yesterday -> context.getString(R.string.yesterday)
        DateAgo.ThisWeek  -> context.getString(R.string.this_week)
        DateAgo.LastWeek  -> context.getString(R.string.last_week)
        is DateAgo.Other  -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
    }

    class WrappedHistoryItem(val item: EventWithSong) {
        var isSelected by mutableStateOf(false)
    }

    val filteredEvents = remember(events, query) {
        if (query.text.isEmpty()) events
        else events.mapValues { (_, songs) ->
            songs.filter { event ->
                event.song.song.title.contains(query.text, ignoreCase = true) ||
                        event.song.artists.any { it.name.contains(query.text, ignoreCase = true) }
            }
        }.filterValues { it.isNotEmpty() }
    }

    val filteredRemoteEvents = remember(remoteEvents, query) {
        if (query.text.isEmpty()) remoteEvents
        else remoteEvents.mapValues { (_, songs) ->
            songs.filter { event ->
                event.song.song.title.contains(query.text, ignoreCase = true) ||
                        event.song.artists.any { it.name.contains(query.text, ignoreCase = true) }
            }
        }.filterValues { it.isNotEmpty() }
    }

    val wrappedItems = remember(filteredEvents) {
        filteredEvents.flatMap { it.value }.map { WrappedHistoryItem(it) }
    }.toMutableStateList()

    val wrappedRemoteItems = remember(filteredRemoteEvents) {
        filteredRemoteEvents.flatMap { it.value }.map { WrappedHistoryItem(it) }
    }.toMutableStateList()

    val lazyListState = rememberLazyListState()

    
    val activeCount = if (historySource == HistorySource.REMOTE) wrappedRemoteItems.size else wrappedItems.size

    
    val showControls = historySource == HistorySource.LOCAL && !isSearching && wrappedItems.isNotEmpty()

    
    
    
    
    
    val headerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {

            if (activeCount > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.n_song,
                        activeCount,
                        activeCount
                    ),
                    color      = accentColor,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.padding(horizontal = 20.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            
            run {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf(
                        HistorySource.LOCAL  to stringResource(R.string.local_history),
                        HistorySource.REMOTE to stringResource(R.string.remote_history),
                    ).forEach { (source, label) ->
                        val isSelected = historySource == source
                        androidx.compose.material3.Surface(
                            onClick = {
                                viewModel.historySource.value = source
                            },
                            shape  = RoundedCornerShape(50),
                            color  = if (isSelected) accentColor.copy(alpha = 0.18f)
                            else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) accentColor
                                else rememberAdaptivePanelColor(0.15f),
                            ),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier         = Modifier.padding(horizontal = 18.dp),
                            ) {
                                Text(
                                    text       = label,
                                    color      = if (isSelected) accentColor else rememberAdaptiveOnColor().copy(alpha = 0.55f),
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            
            if (showControls) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    
                    androidx.compose.material3.Button(
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = context.getString(R.string.history),
                                    items = wrappedItems.map { it.item.song.toMediaItem() }
                                )
                            )
                        },
                        modifier = Modifier.weight(1.4f).height(52.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                    ) {
                        Icon(
                            painterResource(R.drawable.play), null,
                            tint     = Color(0xFF071520),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.play),
                            color      = Color(0xFF071520),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                    }
                    
                    androidx.compose.material3.Button(
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = context.getString(R.string.history),
                                    items = wrappedItems.map { it.item.song.toMediaItem() }.shuffled()
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.12f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, accentColor.copy(alpha = 0.35f)
                        ),
                    ) {
                        Icon(
                            painterResource(R.drawable.shuffle), null,
                            tint     = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.shuffle),
                            color      = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider(
                color    = rememberAdaptivePanelColor(0.08f),
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topBarHeight + pinnedHeight)
        ) {
            
            if (historySource == HistorySource.REMOTE) {
                filteredRemoteEvents.forEach { (dateAgo, songsInGroup) ->
                    val groupIds   = songsInGroup.map { it.event.id }.toSet()
                    val groupItems = wrappedRemoteItems.filter { it.item.event.id in groupIds }

                    stickyHeader(key = "remote_header_${dateAgo}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF071520).copy(alpha = 0.95f))
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text          = dateAgoToString(dateAgo),
                                color         = accentColor,
                                fontSize      = 13.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }

                    items(
                        items = groupItems,
                        key   = { "remote_${it.item.event.id}" }
                    ) { wrappedItem ->
                        val item     = wrappedItem.item
                        val isActive = item.song.id == mediaMetadata?.id
                        val bgColor  = if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(
                                    width = if (isActive) 1.5.dp else 0.dp,
                                    color = if (isActive) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (item.song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                StreamingQueue.radio(item.song.toMediaMetadata())
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong  = item.song,
                                                navController = navController,
                                                onDismiss     = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                .padding(start = 8.dp, end = 0.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier         = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model              = item.song.song.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize(),
                                )
                                if (isActive) {
                                    PlayingIndicatorBox(
                                        isActive = true,
                                        playWhenReady = isPlaying,
                                        color = accentColor,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = item.song.song.title,
                                    color      = if (isActive) accentColor else rememberAdaptiveOnColor(),
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text     = item.song.artists.joinToString { it.name },
                                    color    = rememberAdaptiveOnColor().copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong  = item.song,
                                            navController = navController,
                                            onDismiss     = menuState::dismiss
                                        )
                                    }
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    painter            = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint               = rememberAdaptiveOnColor().copy(alpha = 0.55f),
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                    }
                }
            } else {
                
                filteredEvents.forEach { (dateAgo, songsInGroup) ->
                    
                    val groupIds    = songsInGroup.map { it.event.id }.toSet()
                    val groupItems  = wrappedItems.filter { it.item.event.id in groupIds }

                    stickyHeader(key = "header_${dateAgo}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF071520).copy(alpha = 0.95f))
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text          = dateAgoToString(dateAgo),
                                color         = accentColor,
                                fontSize      = 13.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }

                    itemsIndexed(
                        items = groupItems,
                        key   = { _, item -> item.item.event.id }
                    ) { groupIndex, wrappedItem ->
                        val event       = wrappedItem.item
                        val isActive    = event.song.id == mediaMetadata?.id
                        val globalIndex = wrappedItems.indexOfFirst { it.item.event.id == event.event.id }
                            .coerceAtLeast(0)
                        val bgColor  = when {
                            wrappedItem.isSelected && selection -> accentColor.copy(alpha = 0.22f)
                            isActive                            -> accentColor.copy(alpha = 0.15f)
                            else                                -> Color.Transparent
                        }
                        val isLast = groupIndex == groupItems.size - 1

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(
                                    width = if (wrappedItem.isSelected && selection || isActive) 1.5.dp else 0.dp,
                                    color = when {
                                        wrappedItem.isSelected && selection -> accentColor
                                        isActive                            -> accentColor
                                        else                                -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (event.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title      = dateAgoToString(dateAgo),
                                                        items      = wrappedItems.map { it.item.song.toMediaItem() },
                                                        startIndex = globalIndex
                                                    )
                                                )
                                            }
                                        } else {
                                            wrappedItem.isSelected = !wrappedItem.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                            wrappedItems.forEach { it.isSelected = false }
                                            wrappedItem.isSelected = true
                                        }
                                    }
                                )
                                .animateItem()
                                .padding(start = 8.dp, end = 0.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            
                            Box(
                                modifier         = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                val thumb = event.song.song.thumbnailUrl
                                if (thumb != null) {
                                    AsyncImage(
                                        model              = thumb,
                                        contentDescription = null,
                                        contentScale       = ContentScale.Crop,
                                        modifier           = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter            = painterResource(R.drawable.music_note),
                                        contentDescription = null,
                                        tint               = accentColor.copy(alpha = 0.6f),
                                        modifier           = Modifier.size(24.dp),
                                    )
                                }
                                
                                if (isActive) {
                                    PlayingIndicatorBox(
                                        isActive = true,
                                        playWhenReady = isPlaying,
                                        color = accentColor,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                    )
                                }
                                
                                if (wrappedItem.isSelected && selection) {
                                    Box(
                                        modifier         = Modifier
                                            .fillMaxSize()
                                            .background(accentColor.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter            = painterResource(R.drawable.check),
                                            contentDescription = null,
                                            tint               = Color.White,
                                            modifier           = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = event.song.song.title,
                                    color      = if (isActive) accentColor else rememberAdaptiveOnColor(),
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text     = event.song.artists.joinToString { it.name },
                                    color    = rememberAdaptiveOnColor().copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (!selection) {
                                        menuState.show {
                                            SongMenu(
                                                originalSong  = event.song,
                                                event         = event.event,
                                                navController = navController,
                                                onDismiss     = menuState::dismiss
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    painter            = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint               = rememberAdaptiveOnColor().copy(alpha = 0.55f),
                                )
                            }
                        }

                        if (!isLast) Spacer(Modifier.height(6.dp))
                        else         Spacer(Modifier.height(16.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }

        
        
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topBarHeight)
                .onGloballyPositioned { coords ->
                    pinnedHeight = with(density) { coords.size.height.toDp() }
                }
        ) {
            headerContent()
        }

        
        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedItems.count { it.isSelected }
                        Text(
                            text  = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleLarge,
                            color = rememberAdaptiveOnColor(),
                        )
                    }
                    isSearching -> {
                        TextField(
                            value         = query,
                            onValueChange = { query = it },
                            placeholder   = {
                                Text(
                                    text  = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = rememberAdaptiveOnColor().copy(alpha = 0.4f),
                                )
                            },
                            singleLine     = true,
                            textStyle      = MaterialTheme.typography.titleLarge.copy(color = rememberAdaptiveOnColor()),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors         = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor  = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    else -> {
                        
                        Text(
                            text  = stringResource(R.string.history),
                            style = MaterialTheme.typography.titleLarge,
                            color = rememberAdaptiveOnColor(),
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        when {
                            isSearching -> { isSearching = false; query = TextFieldValue() }
                            selection   -> selection = false
                            else        -> navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) navController.backToMain()
                    }
                ) {
                    Icon(
                        painter            = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null,
                        tint               = rememberAdaptiveOnColor(),
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedItems.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedItems.size) wrappedItems.forEach { it.isSelected = false }
                            else wrappedItems.forEach { it.isSelected = true }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedItems.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null,
                            tint               = rememberAdaptiveOnColor(),
                        )
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = wrappedItems
                                        .filter { it.isSelected }
                                        .map { it.item.song.toMediaItem().metadata!! },
                                    onDismiss    = menuState::dismiss,
                                    clearAction  = { selection = false },
                                    currentItems = emptyList()
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint               = rememberAdaptiveOnColor(),
                        )
                    }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            painter            = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint               = rememberAdaptiveOnColor(),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier.onGloballyPositioned { coords ->
                topBarHeight = with(density) { coords.size.height.toDp() }
            }
        )
    }
}