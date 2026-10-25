/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.library

import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.StreamingModeKey
import com.betsy.music.db.entities.Playlist
import com.betsy.music.ui.component.CreatePlaylistDialog
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.menu.AddSongsToPlaylistSheet
import com.betsy.music.ui.menu.PlaylistMenu
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.rememberGuestId
import com.betsy.music.utils.rememberPreference
import com.betsy.music.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CardBg: Color
    @Composable get() = com.betsy.music.ui.theme.rememberAdaptiveOnColor().copy(alpha = 0.08f)
private val SubText: Color
    @Composable get() = com.betsy.music.ui.theme.rememberAdaptiveOnColor().copy(alpha = 0.55f)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit = {},
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState      = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    val context        = LocalContext.current
    val adaptiveColor  = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    val playlists by viewModel.allPlaylists.collectAsState()
    val topSize   by viewModel.topValue.collectAsState(initial = 50)
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor      = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val (isStreamingMode) = rememberPreference(StreamingModeKey, defaultValue = true)

    
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }
    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val guestId = rememberGuestId()
    val currentOwnerId = remember(prefs, guestId) {
        val activeId = prefs[activeAccountIdKey] ?: ""
        activeId.ifBlank { guestId }
    }
    val isLoggedIn = remember(currentOwnerId) {
        currentOwnerId.isNotBlank() && !currentOwnerId.startsWith("guest_")
    }

    
    val visiblePlaylists = remember(playlists, currentOwnerId, isStreamingMode) {
        playlists.filter { pl ->
            val isOwner     = pl.playlist.ownerId.isBlank() || pl.playlist.ownerId == currentOwnerId
            val isModeMatch = pl.playlist.isMixed || pl.playlist.isLocalMode == !isStreamingMode
            isOwner && isModeMatch
        }
    }

    
    val hiddenOtherModeCount = remember(playlists, currentOwnerId, isStreamingMode) {
        playlists.count { pl ->
            val isOwner      = pl.playlist.ownerId.isBlank() || pl.playlist.ownerId == currentOwnerId
            val isWrongMode  = !pl.playlist.isMixed && pl.playlist.isLocalMode == isStreamingMode
            isOwner && isWrongMode
        }
    }

    val database = LocalDatabase.current

    
    LaunchedEffect(isLoggedIn, currentOwnerId) {
        if (isLoggedIn && currentOwnerId.isNotBlank() && guestId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                database.transaction {
                    migratePlaylistOwner(fromOwnerId = guestId, toOwnerId = currentOwnerId)
                }
            }
        }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    val strBack        = stringResource(R.string.back)
    val strTitle       = stringResource(R.string.lib_playlists_title)
    val strMyPlaylists = stringResource(R.string.lib_my_playlists)
    val strLiked       = stringResource(R.string.liked)
    val strOffline     = stringResource(R.string.offline)
    val strMyTop       = stringResource(R.string.my_top)
    val strCreateFab   = stringResource(R.string.lib_create_playlist)
    val strNoPlaylists = stringResource(R.string.lib_no_playlists)
    val strTapToStart  = stringResource(R.string.lib_tap_to_start)
    var topBarHeightPx by remember { mutableStateOf(0) }
    val topBarHeightDp = with(LocalDensity.current) { topBarHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            state          = rememberLazyListState(),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier       = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                ),
        ) {


            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isStreamingMode) accentColor.copy(alpha = 0.15f)
                                else Color(0xFF2A5C3A).copy(alpha = 0.4f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isStreamingMode) Icons.Default.Download else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint     = if (isStreamingMode) accentColor else Color(0xFF4CAF50),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text       = if (isStreamingMode) stringResource(R.string.lib_mode_streaming) else stringResource(R.string.lib_mode_local),
                            fontSize   = 10.sp,
                            color      = if (isStreamingMode) accentColor else Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text     = "${visiblePlaylists.size + if (isStreamingMode) 3 else 2}",
                        fontSize = 13.sp,
                        color    = accentColor,
                    )
                }
            }

            
            item {
                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AutoPlaylistRow(
                        icon    = Icons.Default.Favorite,
                        label   = strLiked,
                        tint    = Color(0xFFFF006E),
                        onClick = { navController.navigate("auto_playlist/liked") },
                    )
                    
                    if (isStreamingMode) {
                        AutoPlaylistRow(
                            icon    = Icons.Default.Download,
                            label   = strOffline,
                            tint    = accentColor,
                            onClick = { navController.navigate("auto_playlist/downloaded") },
                        )
                    }
                    AutoPlaylistRow(
                        icon    = Icons.Default.Star,
                        label   = "$strMyTop $topSize",
                        tint    = Color(0xFFFFD700),
                        onClick = { navController.navigate("top_playlist/$topSize") },
                    )
                }
            }

            
            item {
                if (hiddenOtherModeCount > 0) {
                    OtherModeBanner(
                        accentColor     = accentColor,
                        count           = hiddenOtherModeCount,
                        isStreamingMode = isStreamingMode,
                        modifier        = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            
            item {
                if (visiblePlaylists.isEmpty()) {
                    EmptyPlaylistsHint(
                        modifier       = Modifier.padding(vertical = 48.dp),
                        onCreateClick  = { showCreatePlaylistDialog = true },
                        strNoPlaylists = strNoPlaylists,
                        strTapToStart  = strTapToStart,
                        strCreate      = strCreateFab,
                        accentColor    = accentColor,
                    )
                } else {
                    Text(
                        text       = strMyPlaylists,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = adaptiveColor.copy(alpha = 0.8f),
                        modifier   = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
            }

            
            items(items = visiblePlaylists, key = { it.id }) { playlist ->
                UserPlaylistRow(
                    playlist       = playlist,
                    menuState      = menuState,
                    navController  = navController,
                    coroutineScope = coroutineScope,
                    accentColor    = accentColor,
                    modifier       = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp),
                )
            }

            item { Spacer(Modifier.height(96.dp)) }
        }

        
        TopAppBar(
            title = { Text(strTitle, color = adaptiveColor) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = strBack, tint = adaptiveColor)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier.onGloballyPositioned { coordinates ->
                topBarHeightPx = coordinates.size.height
            },
        )

        
        FloatingActionButton(
            onClick        = { showCreatePlaylistDialog = true },
            shape          = CircleShape,
            containerColor = accentColor,
            contentColor   = Color.Black,
            elevation      = FloatingActionButtonDefaults.elevation(0.dp),
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
                .padding(end = 24.dp, bottom = 16.dp)
                .size(56.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = strCreateFab, modifier = Modifier.size(26.dp))
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss             = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing          = isLoggedIn,
        )
    }
}


@Composable
private fun OtherModeBanner(
    accentColor     : Color,
    count           : Int,
    isStreamingMode : Boolean,
    modifier        : Modifier = Modifier,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.09f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier         = Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = if (isStreamingMode) Icons.Default.MusicNote else Icons.Default.Download,
                contentDescription = null,
                tint               = accentColor,
                modifier           = Modifier.size(22.dp),
            )
        }
        Text(
            text     = stringResource(R.string.playlists_hidden_in_mode, count, stringResource(if (isStreamingMode) R.string.lib_mode_local else R.string.lib_mode_streaming)),
            fontSize = 12.sp,
            color    = adaptiveColor.copy(alpha = 0.60f),
            modifier = Modifier.weight(1f),
        )
    }
}


@Composable
private fun AutoPlaylistRow(
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    label  : String,
    tint   : Color,
    onClick: () -> Unit,
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = adaptiveColor)
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun UserPlaylistRow(
    playlist       : Playlist,
    menuState      : com.betsy.music.ui.component.MenuState,
    navController  : NavController,
    coroutineScope : CoroutineScope,
    accentColor    : Color    = Color(0xFF3b9cac),
    modifier       : Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    var showMenu         by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsSheet by remember { mutableStateOf(false) }

    val strOptions    = stringResource(R.string.lib_options)
    val strRename     = stringResource(R.string.lib_rename)
    val strDeletePl   = stringResource(R.string.lib_delete_playlist)
    val strRenameTitle = stringResource(R.string.lib_rename_playlist_title)
    val strSave       = stringResource(R.string.lib_save)
    val strCancel     = stringResource(R.string.lib_cancel)
    val strDelete     = stringResource(R.string.lib_delete)
    val strDeleteMsg  = stringResource(R.string.lib_delete_confirm_msg, playlist.playlist.name)
    val strSongsCount = pluralStringResource(R.plurals.lib_songs_count, playlist.songCount, playlist.songCount)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .combinedClickable(
                onClick     = { navController.navigate("local_playlist/${playlist.id}") },
                onLongClick = {
                    menuState.show {
                        PlaylistMenu(
                            playlist       = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss      = menuState::dismiss,
                        )
                    }
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.QueueMusic,
                contentDescription = null,
                tint               = accentColor.copy(alpha = 0.6f),
                modifier           = Modifier.size(28.dp),
            )
            if (playlist.thumbnails.isNotEmpty()) {
                AsyncImage(
                    model              = playlist.thumbnails.first(),
                    contentDescription = playlist.playlist.name,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop,
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = playlist.playlist.name,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = adaptiveColor,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(text = strSongsCount, fontSize = 13.sp, color = SubText)
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = strOptions, tint = adaptiveColor.copy(alpha = 0.6f))
            }
            DropdownMenu(
                expanded         = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor   = Color(0xFF0A2F3D).copy(alpha = 0.97f),
                shape            = RoundedCornerShape(16.dp),
                modifier         = Modifier.width(220.dp),
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Edit, null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Text(strRename, color = adaptiveColor, fontSize = 14.sp)
                        }
                    },
                    onClick = { showMenu = false; showRenameDialog = true },
                )
                HorizontalDivider(color = adaptiveColor.copy(alpha = 0.08f))
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.add_songs), color = adaptiveColor, fontSize = 14.sp)
                        }
                    },
                    onClick = { showMenu = false; showAddSongsSheet = true },
                )
                HorizontalDivider(color = adaptiveColor.copy(alpha = 0.08f))
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                            Text(strDeletePl, color = Color(0xFFFF5252), fontSize = 14.sp)
                        }
                    },
                    onClick = { showMenu = false; showDeleteDialog = true },
                )
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(playlist.playlist.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor   = Color(0xFF0A2F3D).copy(alpha = 0.95f),
            title            = { Text(strRenameTitle, color = Color.White, fontWeight = FontWeight.Bold) },
            text             = {
                OutlinedTextField(
                    value         = newName,
                    onValueChange = { newName = it },
                    singleLine    = true,
                    colors        = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = accentColor,
                        unfocusedBorderColor    = Color.White.copy(alpha = 0.3f),
                        focusedTextColor        = Color.White,
                        unfocusedTextColor      = Color.White,
                        cursorColor             = accentColor,
                        focusedContainerColor   = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            database.query {
                                update(playlist.playlist.copy(name = newName, lastUpdateTime = java.time.LocalDateTime.now()))
                            }
                            coroutineScope.launch(Dispatchers.IO) {
                                playlist.playlist.browseId?.let { com.betsy.audius.compat.AudiusApi.renamePlaylist(it, newName) }
                            }
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape  = RoundedCornerShape(10.dp),
                ) { Text(strSave, color = Color.Black, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(strCancel, color = Color.White.copy(alpha = 0.6f))
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = Color(0xFF0A2F3D).copy(alpha = 0.95f),
            title            = { Text(strDeletePl, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
            text             = { Text(strDeleteMsg, color = Color.White.copy(alpha = 0.8f)) },
            confirmButton    = {
                Button(
                    onClick = {
                        database.transaction { delete(playlist.playlist) }
                        coroutineScope.launch(Dispatchers.IO) {
                            playlist.playlist.browseId?.let { com.betsy.audius.compat.AudiusApi.deletePlaylist(it) }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape  = RoundedCornerShape(10.dp),
                ) { Text(strDelete, color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strCancel, color = Color.White.copy(alpha = 0.6f))
                }
            },
        )
    }

    if (showAddSongsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSongsSheet = false },
            sheetState       = sheetState,
            containerColor   = Color(0xFF111B1D),
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(adaptiveColor.copy(alpha = 0.2f))
                )
            },
        ) {
            AddSongsToPlaylistSheet(
                playlistId = playlist.id,
                onDismiss  = { showAddSongsSheet = false },
            )
        }
    }
}


@Composable
private fun EmptyPlaylistsHint(
    modifier       : Modifier = Modifier,
    onCreateClick  : () -> Unit,
    strNoPlaylists : String,
    strTapToStart  : String,
    strCreate      : String,
    accentColor    : Color    = Color(0xFF3b9cac),
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier         = Modifier.size(100.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.MusicNote,
                contentDescription = null,
                tint               = accentColor.copy(alpha = 0.5f),
                modifier           = Modifier.size(50.dp),
            )
        }
        Text(text = strNoPlaylists, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = adaptiveColor.copy(alpha = 0.6f))
        Text(text = strTapToStart,  fontSize = 14.sp, color = SubText)
        Button(
            onClick = onCreateClick,
            colors  = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape   = RoundedCornerShape(24.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strCreate, color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}