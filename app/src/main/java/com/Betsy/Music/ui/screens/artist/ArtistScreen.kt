/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.artist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.betsy.music.utils.localizedStreamingTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.ArtistItem
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.LocalDatabase
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.LocalPlayerConnection
import com.betsy.music.R
import com.betsy.music.constants.AppBarHeight
import com.betsy.music.db.entities.ArtistEntity
import com.betsy.music.extensions.togglePlayPause
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.ui.component.IconButton
import com.betsy.music.ui.component.LocalMenuState
import com.betsy.music.ui.component.NavigationTitle
import com.betsy.music.ui.component.SongListItem
import com.betsy.music.ui.component.StreamingGridItem
import com.betsy.music.ui.component.StreamingListItem
import com.betsy.music.ui.component.shimmer.ButtonPlaceholder
import com.betsy.music.ui.component.shimmer.ListItemPlaceHolder
import com.betsy.music.ui.component.shimmer.ShimmerHost
import com.betsy.music.ui.component.shimmer.TextPlaceholder
import com.betsy.music.ui.menu.SongMenu
import com.betsy.music.ui.menu.StreamingAlbumMenu
import com.betsy.music.ui.menu.StreamingArtistMenu
import com.betsy.music.ui.menu.StreamingPlaylistMenu
import com.betsy.music.ui.menu.StreamingSongMenu
import com.betsy.music.ui.utils.backToMain
import com.betsy.music.ui.utils.fadingEdge
import com.betsy.music.ui.utils.resize
import com.betsy.music.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.utils.rememberPreference

@SuppressLint("ServiceCast")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(56.dp) }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topBarHeight),
    ) {
        if (artistPage == null) {
            item(key = "shimmer") {
                ShimmerHost {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3),
                    ) {
                        Spacer(
                            modifier = Modifier
                                .shimmer()
                                .background(MaterialTheme.colorScheme.onSurface)
                                .fadingEdge(
                                    top = WindowInsets.systemBars
                                        .asPaddingValues()
                                        .calculateTopPadding() + AppBarHeight,
                                    bottom = 108.dp,
                                ),
                        )
                        TextPlaceholder(
                            height = 56.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 48.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        ButtonPlaceholder(Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        ButtonPlaceholder(Modifier.weight(1f))
                    }

                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        } else {

            item(key = "header") {
                val thumbnail = artistPage.artist.thumbnail
                val artistName = artistPage.artist.title

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            AsyncImage(
                                model = thumbnail.resize(1200, 1200),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .fadingEdge(
                                        top = WindowInsets.systemBars
                                            .asPaddingValues()
                                            .calculateTopPadding() + AppBarHeight,
                                        bottom = 80.dp,
                                    ),
                            )
                        }
                    }

                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        
                        Text(
                            text = artistName,
                            color = rememberAdaptiveOnColor(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        
                        artistPage.artist.subscriberCountText?.let { subscribers ->
                            
                            
                            val subscribersCount = remember(subscribers) { extractSubscriberCount(subscribers) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = accentColor,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (subscribersCount != null)
                                        stringResource(R.string.artist_subscribers_count, subscribersCount)
                                    else subscribers,
                                    fontSize = 13.sp,
                                    color = accentColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        
                        val description = artistPage.description ?: buildString {
                            append(artistName)
                            append(" is a music artist.")
                        }

                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = rememberAdaptiveOnColor().copy(alpha = 0.60f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(bottom = 16.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )

                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            
                            val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
                            androidx.compose.material3.Button(
                                onClick = {
                                    database.transaction {
                                        val artist = libraryArtist?.artist
                                        if (artist != null) {
                                            update(artist.toggleLike())
                                        } else {
                                            artistPage.artist.let {
                                                insert(
                                                    ArtistEntity(
                                                        id = it.id,
                                                        name = it.title,
                                                        channelId = it.channelId,
                                                        thumbnailUrl = it.thumbnail,
                                                    ).toggleLike()
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.4f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (isSubscribed) accentColor else accentColor.copy(alpha = 0.12f)
                                ),
                                border = if (!isSubscribed) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)) else null,
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSubscribed) Color(0xFF071520) else accentColor,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        if (isSubscribed) R.string.subscribed else R.string.subscribe
                                    ),
                                    color = if (isSubscribed) Color(0xFF071520) else accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }


                            
                            artistPage.artist.shuffleEndpoint?.let { shuffleEndpoint ->
                                androidx.compose.material3.Button(
                                    onClick = { playerConnection.playQueue(StreamingQueue(shuffleEndpoint)) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = accentColor.copy(alpha = 0.12f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                                ) {
                                    Icon(painterResource(R.drawable.shuffle), null, tint = accentColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.shuffle), color = accentColor, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            
            if (librarySongs.isNotEmpty()) {
                item {
                    NavigationTitle(
                        title = stringResource(R.string.from_your_library),
                        onClick = {
                            navController.navigate("artist/${viewModel.artistId}/songs")
                        },
                    )
                }

                items(
                    items = librarySongs,
                    key = { "local_${it.id}" },
                ) { song ->
                    SongListItem(
                        song = song,
                        showInLibraryIcon = true,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint = rememberAdaptiveOnColor(),
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            StreamingQueue(
                                                WatchEndpoint(videoId = song.id),
                                                song.toMediaMetadata(),
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            )
                            .animateItem(),
                    )
                }
            }

            
            artistPage.sections.fastForEach { section ->
                if (section.items.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = localizedStreamingTitle(section.title),
                            onClick = section.moreEndpoint?.let {
                                {
                                    navController.navigate(
                                        "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                    )
                                }
                            },
                        )
                    }
                }

                if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                    items(
                        items = section.items,
                        key = { it.id },
                    ) { song ->
                        StreamingListItem(
                            item = song as SongItem,
                            isActive = mediaMetadata?.id == song.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            StreamingSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                        tint = rememberAdaptiveOnColor(),
                                    )
                                }
                            },
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                StreamingQueue(
                                                    WatchEndpoint(videoId = song.id),
                                                    song.toMediaMetadata()
                                                ),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            StreamingSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }
                } else {
                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = section.items,
                                key = { it.id },
                            ) { item ->
                                StreamingGridItem(
                                    item = item,
                                    isActive = when (item) {
                                        is SongItem -> mediaMetadata?.id == item.id
                                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                                        else -> false
                                    },
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                when (item) {
                                                    is SongItem -> playerConnection.playQueue(
                                                        StreamingQueue(
                                                            WatchEndpoint(videoId = item.id),
                                                            item.toMediaMetadata()
                                                        ),
                                                    )
                                                    is AlbumItem -> {
                                                        try {
                                                            if (item.id.isNotEmpty()) {
                                                                navController.navigate("album/${item.id}")
                                                            } else {
                                                                Log.w("ArtistScreen", "Album ID is empty")
                                                                Toast.makeText(
                                                                    context,
                                                                    context.getString(R.string.navigation_error),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("ArtistScreen", "Navigation error to album: ${item.id}", e)
                                                            Toast.makeText(
                                                                context,
                                                                context.getString(R.string.navigation_error),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                    is ArtistItem -> {
                                                        try {
                                                            if (item.id.isNotEmpty()) {
                                                                navController.navigate("artist/${item.id}")
                                                            } else {
                                                                Log.w("ArtistScreen", "Artist ID is empty")
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("ArtistScreen", "Navigation error to artist: ${item.id}", e)
                                                        }
                                                    }
                                                    is PlaylistItem -> {
                                                        try {
                                                            if (item.id.isNotEmpty()) {
                                                                navController.navigate("online_playlist/${item.id}")
                                                            } else {
                                                                Log.w("ArtistScreen", "Playlist ID is empty")
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("ArtistScreen", "Navigation error to playlist: ${item.id}", e)
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    when (item) {
                                                        is SongItem -> StreamingSongMenu(
                                                            song = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                        is AlbumItem -> StreamingAlbumMenu(
                                                            albumItem = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                        is ArtistItem -> StreamingArtistMenu(
                                                            artist = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                        is PlaylistItem -> StreamingPlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    
    TopAppBar(
        title = {
            if (!transparentAppBar)
                Text(artistPage?.artist?.title.orEmpty(), color = rememberAdaptiveOnColor())
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                    tint = rememberAdaptiveOnColor(),
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(
                    painterResource(R.drawable.link),
                    contentDescription = null,
                    tint = rememberAdaptiveOnColor(),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = Modifier.onGloballyPositioned { coords ->
            topBarHeight = with(density) { coords.size.height.toDp() }
        }
    )
}

   
                                                                                  
                                                                                              
                                                                                              
   
private val SUBSCRIBER_COUNT_REGEX = Regex(
    "^\\s*(\\d[\\d.,\\u00A0\\u202F]*(?:\\s?(?:mill\\.?|mil|Mio\\.?|Mrd\\.?|[KkMmBb])(?![\\p{L}]))?)"
)

private fun extractSubscriberCount(raw: String): String? =
    SUBSCRIBER_COUNT_REGEX.find(raw)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }