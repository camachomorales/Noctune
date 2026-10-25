/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.R
import com.betsy.music.db.entities.Album
import com.betsy.music.db.entities.Artist
import com.betsy.music.db.entities.Playlist
import com.betsy.music.ui.menu.AlbumMenu
import com.betsy.music.ui.menu.ArtistMenu
import com.betsy.music.ui.menu.PlaylistMenu
import com.betsy.music.ui.menu.StreamingPlaylistMenu
import com.betsy.music.utils.iaIdentifierOf
import com.betsy.music.utils.albumRoute
import com.betsy.music.utils.isIaAlbumId
import com.betsy.music.utils.isProxyAlbumId
import kotlinx.coroutines.CoroutineScope
import com.betsy.music.ui.theme.rememberAdaptiveOnColor

@Composable
fun LibraryArtistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    modifier: Modifier = Modifier
) = ArtistListItem(
    artist = artist,
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    ArtistMenu(
                        originalArtist = artist,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null, tint = rememberAdaptiveOnColor()
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            navController.navigate("artist/${artist.id}")
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryArtistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    modifier: Modifier = Modifier
) = ArtistGridItem(
    artist = artist,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                navController.navigate("artist/${artist.id}")
            },
            onLongClick = {
                menuState.show {
                    ArtistMenu(
                        originalArtist = artist,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        )
)

@Composable
fun LibraryAlbumListItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false
) = AlbumListItem(
    album = album,
    isActive = isActive,
    isPlaying = isPlaying,
    trailingContent = {
        
        if (!isProxyAlbumId(album.id)) {
            androidx.compose.material3.IconButton(
                onClick = {
                    menuState.show {
                        AlbumMenu(
                            originalAlbum = album,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null, tint = rememberAdaptiveOnColor()
                )
            }
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            navController.navigate(albumRoute(album.id))
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryAlbumGridItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false
) = AlbumGridItem(
    album = album,
    isActive = isActive,
    isPlaying = isPlaying,
    coroutineScope = coroutineScope,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                navController.navigate(albumRoute(album.id))
            },
            onLongClick = {
                if (!isProxyAlbumId(album.id)) {
                    menuState.show {
                        AlbumMenu(
                            originalAlbum = album,
                            navController = navController,
                            onDismiss = menuState::dismiss
                        )
                    }
                }
            }
        )
)

@Composable
fun LibraryPlaylistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier
) = PlaylistListItem(
    playlist = playlist,
    trailingContent = {
        androidx.compose.material3.IconButton(
            onClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            StreamingPlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null, tint = rememberAdaptiveOnColor()
            )
        }
    },
    modifier = modifier
        .fillMaxWidth()
        .clickable {
            if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0)
                navController.navigate("online_playlist/${playlist.playlist.browseId}")
            else
                navController.navigate("local_playlist/${playlist.id}")
        }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier,
    context: Context 
) = PlaylistGridItem(
    playlist = playlist,
    fillMaxWidth = true,
    modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0)
                    navController.navigate("online_playlist/${playlist.playlist.browseId}")
                else
                    navController.navigate("local_playlist/${playlist.id}")
            },
            onLongClick = {
                menuState.show {
                    if (playlist.playlist.isEditable || playlist.songCount != 0) {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss
                        )
                    } else {
                        playlist.playlist.browseId?.let { browseId ->
                            StreamingPlaylistMenu(
                                playlist = PlaylistItem(
                                    id = browseId,
                                    title = playlist.playlist.name,
                                    author = null,
                                    songCountText = null,
                                    thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                                    playEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.playEndpointParams
                                    ),
                                    shuffleEndpoint = WatchEndpoint(
                                        playlistId = browseId,
                                        params = playlist.playlist.shuffleEndpointParams
                                    ),
                                    radioEndpoint = WatchEndpoint(
                                        playlistId = "RDAMPL$browseId",
                                        params = playlist.playlist.radioEndpointParams
                                    ),
                                    isEditable = false
                                ),
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            }
        ),
    context = LocalContext.current 
)