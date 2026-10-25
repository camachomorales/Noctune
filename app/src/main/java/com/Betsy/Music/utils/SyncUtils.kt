/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.utils

import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.ArtistItem
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.utils.completed
import com.betsy.audius.compat.utils.completedLibraryPage
import com.betsy.music.db.musicDatabase
import com.betsy.music.db.entities.ArtistEntity
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.music.db.entities.PlaylistSongMap
import com.betsy.music.db.entities.SongEntity
import com.betsy.music.models.toMediaMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SyncUtils @Inject constructor(
    val database: musicDatabase,
) {
    suspend fun syncLikedSongs() {
        AudiusApi.playlist("LM").completed().onSuccess { page ->
            val songs = page.songs.reversed()

            database.likedSongsByNameAsc().first()
                .filterNot { it.id in songs.map(SongItem::id) }
                .forEach { database.update(it.song.localToggleLike()) }

            songs.forEach { song ->
                val dbSong = database.song(song.id).firstOrNull()
                database.transaction {
                    when (dbSong) {
                        null -> insert(song.toMediaMetadata(), SongEntity::localToggleLike)
                        else -> if (!dbSong.song.liked) update(dbSong.song.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncLibrarySongs() {
        AudiusApi.library("FEmusic_liked_videos").completedLibraryPage().onSuccess { page ->
            val songs = page.items.filterIsInstance<SongItem>().reversed()

            database.songsByNameAsc().first()
                .filterNot { it.id in songs.map(SongItem::id) }
                .forEach { database.update(it.song.toggleLibrary()) }

            songs.forEach { song ->
                val dbSong = database.song(song.id).firstOrNull()
                database.transaction {
                    when (dbSong) {
                        null -> insert(song.toMediaMetadata(), SongEntity::toggleLibrary)
                        else -> if (dbSong.song.inLibrary == null) update(dbSong.song.toggleLibrary())
                    }
                }
            }
        }
    }

    suspend fun syncLikedAlbums() {
        AudiusApi.library("FEmusic_liked_albums").completedLibraryPage().onSuccess { page ->
            val albums = page.items.filterIsInstance<AlbumItem>().reversed()

            database.albumsLikedByNameAsc().first()
                .filterNot { it.id in albums.map(AlbumItem::id) }
                .forEach { database.update(it.album.localToggleLike()) }

            albums.forEach { album ->
                val dbAlbum = database.album(album.id).firstOrNull()
                AudiusApi.album(album.browseId).onSuccess { albumPage ->
                    when (dbAlbum) {
                        null -> {
                            database.insert(albumPage)
                            database.album(album.id).firstOrNull()?.let {
                                database.update(it.album.localToggleLike())
                            }
                        }
                        else -> if (dbAlbum.album.bookmarkedAt == null)
                            database.update(dbAlbum.album.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncArtistsSubscriptions() {
        AudiusApi.library("FEmusic_library_corpus_artists").completedLibraryPage().onSuccess { page ->
            val artists = page.items.filterIsInstance<ArtistItem>()

            database.artistsBookmarkedByNameAsc().first()
                .filterNot { it.id in artists.map(ArtistItem::id) }
                .forEach { database.update(it.artist.localToggleLike()) }

            artists.forEach { artist ->
                val dbArtist = database.artist(artist.id).firstOrNull()
                database.transaction {
                    when (dbArtist) {
                        null -> {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.channelId,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        }
                        else -> if (dbArtist.artist.bookmarkedAt == null)
                            update(dbArtist.artist.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncSavedPlaylists() {
        AudiusApi.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess { page ->
            val playlistList = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" ||  it.id == "SE" }
                .reversed()
            val dbPlaylists = database.playlistsByNameAsc().first()

            dbPlaylists.filterNot { it.playlist.browseId in playlistList.map(PlaylistItem::id) }
                .filterNot { it.playlist.browseId == null }
                .forEach { database.update(it.playlist.localToggleLike()) }

            playlistList.onEach { playlist ->
                var playlistEntity = dbPlaylists.find { playlist.id == it.playlist.browseId }?.playlist
                if (playlistEntity == null) {
                    playlistEntity = PlaylistEntity(
                        name = playlist.title,
                        browseId = playlist.id,
                        isEditable = playlist.isEditable,
                        bookmarkedAt = LocalDateTime.now(),
                        remoteSongCount = playlist.songCountText?.let {
                            Regex("""\d+""").find(it)?.value?.toIntOrNull()
                        }
                    )

                    database.insert(playlistEntity)
                } else database.update(playlistEntity, playlist)

                syncPlaylist(playlist.id, playlistEntity.id)
            }
        }
    }

    suspend fun syncPlaylist(browseId: String, playlistId: String) {
        val playlistPage = AudiusApi.playlist(browseId).completed().getOrNull() ?: return
        database.transaction {
            clearPlaylist(playlistId)
            playlistPage.songs
                .map(SongItem::toMediaMetadata)
                .onEach { insert(it) }
                .mapIndexed { position, song ->
                    PlaylistSongMap(
                        songId = song.id,
                        playlistId = playlistId,
                        position = position,
                        setVideoId = song.setVideoId
                    )
                }.forEach { insert(it) }
        }
    }
}