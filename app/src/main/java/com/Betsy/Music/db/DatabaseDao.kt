/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.db

import androidx.room.*
import com.betsy.music.db.entities.PlaylistEntity
import com.betsy.audius.compat.models.PlaylistItem
import com.betsy.audius.compat.pages.AlbumPage
import com.betsy.audius.compat.pages.ArtistPage
import com.betsy.music.constants.*
import com.betsy.music.db.entities.*
import com.betsy.music.models.MediaMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime


@Dao
interface DatabaseDao {

    

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: SongArtistMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: SongAlbumMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: AlbumArtistMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: PlaylistSongMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistory)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: Event)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: RelatedSongMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playCount: PlayCountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setVideoId: SetVideoIdEntity)

    

    @Update
    suspend fun update(song: SongEntity)

    @Update
    suspend fun update(artist: ArtistEntity)

    @Update
    suspend fun update(album: AlbumEntity)

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Update
    suspend fun update(map: PlaylistSongMap)

    

    @Delete
    suspend fun delete(song: SongEntity)

    @Delete
    suspend fun delete(map: SongArtistMap)

    @Delete
    suspend fun delete(artist: ArtistEntity)

    @Delete
    suspend fun delete(album: AlbumEntity)

    @Delete
    suspend fun delete(map: AlbumArtistMap)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(map: PlaylistSongMap)

    @Delete
    suspend fun delete(lyrics: LyricsEntity)

    @Delete
    suspend fun delete(searchHistory: SearchHistory)

    @Delete
    suspend fun delete(event: Event)

    

    @Query("UPDATE playlist SET ownerId = :toOwnerId WHERE ownerId = '' OR ownerId IS NULL")
    suspend fun claimOrphanPlaylists(toOwnerId: String)

    @Query("UPDATE playlist SET ownerId = :toOwnerId WHERE ownerId = :fromOwnerId")
    suspend fun migratePlaylistOwner(fromOwnerId: String, toOwnerId: String)

    

    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY rowId")
    fun songsByRowIdAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY inLibrary")
    fun songsByCreateDateAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY title")
    fun songsByNameAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY totalPlayTime")
    fun songsByPlayTimeAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE liked ORDER BY rowId")
    fun likedSongsByRowIdAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE liked ORDER BY likedDate")
    fun likedSongsByCreateDateAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE liked ORDER BY title")
    fun likedSongsByNameAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE liked ORDER BY totalPlayTime")
    fun likedSongsByPlayTimeAsc(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE id = :songId")
    fun song(songId: String): Flow<Song?>

    @Query("SELECT * FROM Song WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): Song?

    @Query("SELECT * FROM song")
    fun allSongs(): Flow<List<Song>>

    @Query("SELECT COUNT(1) FROM song WHERE liked")
    fun likedSongsCount(): Flow<Int>

    @Transaction
    @Query("SELECT song.* FROM song JOIN song_album_map ON song.id = song_album_map.songId WHERE song_album_map.albumId = :albumId")
    fun albumSongs(albumId: String): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId ORDER BY position")
    fun playlistSongs(playlistId: String): Flow<List<PlaylistSong>>

    
    @Query("SELECT * FROM playlist_song_map WHERE songId = :songId")
    suspend fun playlistSongMaps(songId: String): List<PlaylistSongMap>

    
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId ORDER BY position LIMIT -1 OFFSET :offset")
    suspend fun playlistSongMaps(playlistId: String, offset: Int): List<PlaylistSongMap>

    @Transaction
    @Query("""
        SELECT DISTINCT song.* FROM song
        JOIN playlist_song_map ON song.id = playlist_song_map.songId
        JOIN playlist ON playlist_song_map.playlistId = playlist.id
        WHERE playlist.isEditable = 1
        ORDER BY song.rowId DESC
    """)
    fun songsInUserPlaylists(): Flow<List<Song>>

    @Query("UPDATE playCount SET count = count + 1 WHERE song = :songId AND year = :year AND month = :month")
    suspend fun incrementPlayCount(songId: String, year: Int, month: Int)

    @Query("UPDATE song SET totalPlayTime = totalPlayTime + :playTime WHERE id = :songId")
    suspend fun incrementTotalPlayTime(songId: String, playTime: Long)

    @Query("UPDATE song SET inLibrary = :inLibrary WHERE id = :songId")
    suspend fun inLibrary(songId: String, inLibrary: LocalDateTime?)

    suspend fun incrementPlayCount(songId: String) {
        val now = LocalDateTime.now()
        incrementPlayCount(songId, now.year, now.monthValue)
    }

    

    @Transaction
    @Query("""
        SELECT DISTINCT artist.*, 
        (SELECT COUNT(1) FROM song_artist_map sam JOIN song s ON sam.songId = s.id WHERE sam.artistId = artist.id AND s.inLibrary IS NOT NULL) AS songCount
        FROM artist
        JOIN song_artist_map ON artist.id = song_artist_map.artistId
        JOIN song ON song_artist_map.songId = song.id
        JOIN playlist_song_map ON song.id = playlist_song_map.songId
        JOIN playlist ON playlist_song_map.playlistId = playlist.id
        WHERE playlist.isEditable = 1
        ORDER BY artist.rowId DESC
    """)
    fun artistsInUserPlaylists(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT *,
        (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
        WHERE (songCount > 0 OR bookmarkedAt IS NOT NULL)
        ORDER BY rowId DESC
    """)
    fun artistsInAA(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist WHERE songCount > 0 ORDER BY rowId
    """)
    fun artistsByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist WHERE songCount > 0 ORDER BY name
    """)
    fun artistsByNameAsc(): Flow<List<Artist>>

    @Query("SELECT * FROM artist WHERE songCount > 0 ORDER BY songCount")
    fun artistsBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT artist.*,
        (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
        JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
             FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
             GROUP BY artistId ORDER BY totalPlayTime) ON artist.id = artistId
        WHERE songCount > 0
    """)
    fun artistsByPlayTimeAsc(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt
    """)
    fun artistsBookmarkedByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY name
    """)
    fun artistsBookmarkedByNameAsc(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY songCount
    """)
    fun artistsBookmarkedBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @Query("""
        SELECT artist.*,
        (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
         WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
        JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
             FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
             GROUP BY artistId ORDER BY totalPlayTime) ON artist.id = artistId
        WHERE bookmarkedAt IS NOT NULL
    """)
    fun artistsBookmarkedByPlayTimeAsc(): Flow<List<Artist>>

    @Query("SELECT COUNT(*) FROM song_artist_map WHERE artistId = :artistId")
    suspend fun getSongCountForArtist(artistId: String): Int

    @Transaction
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE id = :id")
    fun artist(id: String): Flow<Artist?>

    @Query("SELECT * FROM artist WHERE name = :name")
    suspend fun artistByName(name: String): ArtistEntity?

    @Query("UPDATE artist SET songCount = :count WHERE id = :artistId")
    suspend fun updateArtistSongCount(artistId: String, count: Int)

    suspend fun updateArtistSongsCount(artistId: String) {
        val count = getSongCountForArtist(artistId)
        updateArtistSongCount(artistId, count)
    }

    

    @Transaction
    @Query("""
        SELECT *,
        (SELECT COUNT(1) FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM album
        WHERE (songCount > 0 OR album.bookmarkedAt IS NOT NULL)
        ORDER BY rowId DESC
    """)
    fun albumsInAA(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY rowId")
    fun albumsByCreateDateAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY title")
    fun albumsByNameAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY year")
    fun albumsByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY songCount")
    fun albumsBySongCountAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY duration")
    fun albumsByLengthAsc(): Flow<List<Album>>

    @Query("""
        SELECT album.*
        FROM album
        JOIN song ON song.albumId = album.id
        WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL)
        GROUP BY album.id
        ORDER BY SUM(song.totalPlayTime)
    """)
    fun albumsByPlayTimeAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY rowId")
    fun albumsLikedByCreateDateAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY title")
    fun albumsLikedByNameAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY year")
    fun albumsLikedByYearAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun albumsLikedBySongCountAsc(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY duration")
    fun albumsLikedByLengthAsc(): Flow<List<Album>>

    @Query("""
        SELECT album.*
        FROM album
        JOIN song ON song.albumId = album.id
        WHERE bookmarkedAt IS NOT NULL
        GROUP BY album.id
        ORDER BY SUM(song.totalPlayTime)
    """)
    fun albumsLikedByPlayTimeAsc(): Flow<List<Album>>

    @Query("""
        SELECT DISTINCT album.*
        FROM album
        JOIN song ON song.albumId = album.id
        JOIN playlist_song_map ON playlist_song_map.songId = song.id
    """)
    fun albumsInUserPlaylists(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE id = :id")
    fun album(id: String): Flow<Album?>

    @Transaction
    @Query("SELECT * FROM album WHERE id = :albumId")
    fun albumWithSongs(albumId: String): Flow<AlbumWithSongs?>

    @Query("SELECT * FROM album_artist_map WHERE albumId = :albumId")
    suspend fun albumArtistMaps(albumId: String): List<AlbumArtistMap>

    

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE bookmarkedAt IS NOT NULL
          AND ownerId = :ownerId
          AND (isLocalMode = :isLocalMode OR isMixed = 1)
        ORDER BY rowId
    """)
    fun playlistsByCreateDateAsc(ownerId: String, isLocalMode: Boolean): Flow<List<Playlist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE bookmarkedAt IS NOT NULL
          AND ownerId = :ownerId
          AND (isLocalMode = :isLocalMode OR isMixed = 1)
        ORDER BY lastUpdateTime
    """)
    fun playlistsByUpdatedDateAsc(ownerId: String, isLocalMode: Boolean): Flow<List<Playlist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE bookmarkedAt IS NOT NULL
          AND ownerId = :ownerId
          AND (isLocalMode = :isLocalMode OR isMixed = 1)
        ORDER BY name
    """)
    fun playlistsByNameAsc(ownerId: String, isLocalMode: Boolean): Flow<List<Playlist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE bookmarkedAt IS NOT NULL
          AND ownerId = :ownerId
          AND (isLocalMode = :isLocalMode OR isMixed = 1)
        ORDER BY songCount
    """)
    fun playlistsBySongCountAsc(ownerId: String, isLocalMode: Boolean): Flow<List<Playlist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE isEditable AND bookmarkedAt IS NOT NULL
          AND ownerId = :ownerId
          AND (isLocalMode = :isLocalMode OR isMixed = 1)
        ORDER BY rowId
    """)
    fun editablePlaylistsByCreateDateAsc(ownerId: String, isLocalMode: Boolean): Flow<List<Playlist>>

    
    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE bookmarkedAt IS NOT NULL
        ORDER BY rowId
    """)
    fun playlistsByCreateDateAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE bookmarkedAt IS NOT NULL
        ORDER BY name
    """)
    fun playlistsByNameAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE id = :playlistId
    """)
    fun playlist(playlistId: String): Flow<Playlist?>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
        WHERE browseId = :browseId
    """)
    fun playlistByBrowseId(browseId: String): Flow<Playlist?>

    

    @Query("SELECT COUNT(*) from playlist_song_map WHERE playlistId = :playlistId AND songId = :songId LIMIT 1")
    suspend fun checkInPlaylist(playlistId: String, songId: String): Int

    @Query("SELECT songId from playlist_song_map WHERE playlistId = :playlistId AND songId IN (:songIds)")
    suspend fun playlistDuplicates(playlistId: String, songIds: List<String>): List<String>

    @Query("""
        UPDATE playlist_song_map SET position = 
            CASE 
                WHEN position < :fromPosition THEN position + 1
                WHEN position > :fromPosition THEN position - 1
                ELSE :toPosition
            END 
        WHERE playlistId = :playlistId AND position BETWEEN MIN(:fromPosition, :toPosition) AND MAX(:fromPosition, :toPosition)
    """)
    suspend fun move(playlistId: String, fromPosition: Int, toPosition: Int)

    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)

    @Query("DELETE FROM playlist WHERE browseId = :browseId")
    suspend fun deletePlaylistById(browseId: String)

    

    @Query("""
        SELECT song.*
        FROM (SELECT n.songId      AS eid,
                     SUM(playTime) AS oldPlayTime,
                     newPlayTime
              FROM event
                       JOIN
                   (SELECT songId, SUM(playTime) AS newPlayTime
                    FROM event
                    WHERE timestamp > (:now - 86400000 * 30 * 1)
                    GROUP BY songId
                    ORDER BY newPlayTime) as n
                   ON event.songId = n.songId
              WHERE timestamp < (:now - 86400000 * 30 * 1)
              GROUP BY n.songId
              ORDER BY oldPlayTime) AS t
                 JOIN song on song.id = t.eid
        WHERE 0.2 * t.oldPlayTime > t.newPlayTime
        LIMIT 100
    """)
    fun forgottenFavorites(now: Long): Flow<List<Song>>
    fun forgottenFavorites(): Flow<List<Song>> = forgottenFavorites(System.currentTimeMillis())

    @Query("""
        SELECT song.*
        FROM (SELECT *, COUNT(1) AS referredCount
              FROM related_song_map
              GROUP BY relatedSongId) map
                 JOIN song ON song.id = map.relatedSongId
        WHERE songId IN (SELECT songId
                         FROM (SELECT songId
                               FROM event
                               ORDER BY ROWID DESC
                               LIMIT 5)
                         UNION
                         SELECT songId
                         FROM (SELECT songId
                               FROM event
                               WHERE timestamp > :now - 86400000 * 7
                               GROUP BY songId
                               ORDER BY SUM(playTime) DESC
                               LIMIT 5)
                         UNION
                         SELECT id
                         FROM (SELECT id
                               FROM song
                               ORDER BY totalPlayTime DESC
                               LIMIT 10))
        ORDER BY referredCount DESC
        LIMIT 100
    """)
    fun quickPicks(now: Long): Flow<List<Song>>

    fun quickPicks(): Flow<List<Song>> = quickPicks(System.currentTimeMillis())

    @Query("""
        SELECT
            song.*
        FROM
            event
        JOIN
            song ON event.songId = song.id
        WHERE
            event.timestamp > (:now - 86400000 * 7 * 2)
        GROUP BY
            song.albumId
        HAVING
            song.albumId IS NOT NULL
        ORDER BY
            sum(event.playTime) DESC
        LIMIT :limit
        OFFSET :offset
    """)
    fun getRecommendationAlbum(now: Long, limit: Int, offset: Int): Flow<List<Song>>

    @Query("""
             SELECT song.id, song.title, song.thumbnailUrl,
               (SELECT COUNT(1)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS songCountListened,
               (SELECT SUM(event.playTime)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS timeListened
        FROM song
        JOIN (SELECT songId
                     FROM event
                     WHERE timestamp > :fromTimeStamp
                     AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)
                     GROUP BY songId
                     ORDER BY SUM(playTime) DESC
                     LIMIT :limit)
        ON song.id = songId
        LIMIT :limit
        OFFSET :offset
    """)
    fun mostPlayedSongsStats(fromTimeStamp: Long, limit: Int, offset: Int = 0, toTimeStamp: Long? = null): Flow<List<SongWithStats>>

    @Query("""
        SELECT song.*,
               (SELECT COUNT(1)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS songCountListened,
               (SELECT SUM(event.playTime)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS timeListened
        FROM song
        JOIN (SELECT songId
                     FROM event
                     WHERE timestamp > :fromTimeStamp
                     AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)
                     GROUP BY songId
                     ORDER BY SUM(playTime) DESC
                     LIMIT :limit)
        ON song.id = songId
        LIMIT :limit
        OFFSET :offset
    """)
    fun mostPlayedSongs(fromTimeStamp: Long, limit: Int, offset: Int = 0, toTimeStamp: Long? = null): Flow<List<Song>>

    @Query("""
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id
                  AND timestamp > :fromTimeStamp AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS songCount,
               (SELECT SUM(event.playTime)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id
                  AND timestamp > :fromTimeStamp AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS timeListened
        FROM artist
                 JOIN(SELECT artistId, SUM(songTotalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN (SELECT songId, SUM(playTime) AS songTotalPlayTime
                                     FROM event
                                     WHERE timestamp > :fromTimeStamp
                                     AND timestamp <= COALESCE(:toTimeStamp, 9999999999999)
                                     GROUP BY songId) AS e
                                    ON song_artist_map.songId = e.songId
                      GROUP BY artistId
                      ORDER BY totalPlayTime DESC
                      LIMIT :limit
                      OFFSET :offset)
                     ON artist.id = artistId
    """)
    fun mostPlayedArtists(fromTimeStamp: Long, limit: Int = -1, offset: Int = 0, toTimeStamp: Long? = null): Flow<List<Artist>>

    @Query("""
    SELECT album.*,
           COUNT(DISTINCT song_album_map.songId) as downloadCount,
           (SELECT COUNT(1)
            FROM song_album_map
                     JOIN event e ON song_album_map.songId = e.songId
            WHERE albumId = album.id
              AND e.timestamp > :fromTimeStamp 
              AND e.timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS songCountListened,
           (SELECT SUM(e.playTime)
            FROM song_album_map
                     JOIN event e ON song_album_map.songId = e.songId
            WHERE albumId = album.id
              AND e.timestamp > :fromTimeStamp 
              AND e.timestamp <= COALESCE(:toTimeStamp, 9999999999999)) AS timeListened
    FROM album
    JOIN song_album_map ON album.id = song_album_map.albumId
    WHERE album.id IN (
        SELECT sam.albumId
        FROM event
                 JOIN song_album_map sam ON event.songId = sam.songId
        WHERE event.timestamp > :fromTimeStamp
          AND event.timestamp <= COALESCE(:toTimeStamp, 9999999999999)
        GROUP BY sam.albumId
        HAVING sam.albumId IS NOT NULL
    )
    GROUP BY album.id
    ORDER BY timeListened DESC
    LIMIT :limit OFFSET :offset
    """)
    fun mostPlayedAlbums(fromTimeStamp: Long, limit: Int = -1, offset: Int = 0, toTimeStamp: Long? = null): Flow<List<Album>>

    @Query("SELECT sum(count) from playCount WHERE song = :songId")
    fun getLifetimePlayCount(songId: String?): Flow<Int>

    @Query("SELECT sum(count) from playCount WHERE song = :songId AND year = :year")
    fun getPlayCountByYear(songId: String?, year: Int): Flow<Int>

    @Query("SELECT count from playCount WHERE song = :songId AND year = :year AND month = :month")
    fun getPlayCountByMonth(songId: String?, year: Int, month: Int): Flow<Int>

    @Query("SELECT count FROM playCount WHERE song = :songId AND year = :year AND month = :month")
    suspend fun getPlayCountByMonthDirect(songId: String, year: Int, month: Int): Int

    @Query("SELECT COUNT(1) FROM related_song_map WHERE songId = :songId LIMIT 1")
    suspend fun hasRelatedSongs(songId: String): Boolean

    @Query("SELECT song.* FROM (SELECT * from related_song_map GROUP BY relatedSongId) map JOIN song ON song.id = map.relatedSongId where songId = :songId")
    fun getRelatedSongs(songId: String): Flow<List<Song>>

    @Query("""
        SELECT song.*
        FROM (SELECT *
              FROM related_song_map
              GROUP BY relatedSongId) map
                 JOIN
             song
             ON song.id = map.relatedSongId
        WHERE songId = :songId
    """)
    suspend fun relatedSongs(songId: String): List<Song>

    

    @Query("SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId ORDER BY inLibrary")
    fun artistSongsInAA(artistId: String): Flow<List<Song>>

    @Query("SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY inLibrary")
    fun artistSongsByCreateDateAsc(artistId: String): Flow<List<Song>>

    @Query("SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY title")
    fun artistSongsByNameAsc(artistId: String): Flow<List<Song>>

    @Query("SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY totalPlayTime")
    fun artistSongsByPlayTimeAsc(artistId: String): Flow<List<Song>>

    @Query("SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL LIMIT :previewSize")
    fun artistSongsPreview(artistId: String, previewSize: Int = 5): Flow<List<Song>>

    

    @Query("SELECT * FROM song WHERE title LIKE '%' || :query || '%' AND inLibrary IS NOT NULL LIMIT :previewSize")
    fun searchSongs(query: String, previewSize: Int): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>> = searchSongs(query, Int.MAX_VALUE)

    @Transaction
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE name LIKE '%' || :query || '%' AND songCount > 0 LIMIT :previewSize")
    fun searchArtists(query: String, previewSize: Int): Flow<List<Artist>>
    fun searchArtists(query: String): Flow<List<Artist>> = searchArtists(query, Int.MAX_VALUE)

    @Transaction
    @Query("SELECT * FROM album WHERE title LIKE '%' || :query || '%' AND EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) LIMIT :previewSize")
    fun searchAlbums(query: String, previewSize: Int): Flow<List<Album>>
    fun searchAlbums(query: String): Flow<List<Album>> = searchAlbums(query, Int.MAX_VALUE)

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE name LIKE '%' || :query || '%' LIMIT :previewSize")
    fun searchPlaylists(query: String, previewSize: Int): Flow<List<Playlist>>
    fun searchPlaylists(query: String): Flow<List<Playlist>> = searchPlaylists(query, Int.MAX_VALUE)

    

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId DESC")
    fun events(): Flow<List<EventWithSong>>

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId ASC LIMIT 1")
    fun firstEvent(): Flow<EventWithSong?>

    
    @Query("SELECT COUNT(*) FROM event")
    fun totalPlayedEventsCount(): Flow<Int>

    
    @Query("SELECT COALESCE(SUM(playTime), 0) FROM event")
    fun totalListenedMillis(): Flow<Long>

    @Query("DELETE FROM event")
    suspend fun clearListenHistory()

    @Query("DELETE FROM search_history WHERE ownerId = :ownerId")
    suspend fun clearSearchHistory(ownerId: String)

    
    @Query("DELETE FROM search_history")
    suspend fun clearAllSearchHistory()

    @Query("SELECT * FROM search_history WHERE ownerId = :ownerId AND `query` LIKE :query || '%' ORDER BY id DESC")
    fun searchHistory(query: String, ownerId: String): Flow<List<SearchHistory>>

    fun searchHistory(ownerId: String): Flow<List<SearchHistory>> = searchHistory("", ownerId)

    

    @Query("SELECT * FROM artist")
    fun allArtistsByPlayTime(): Flow<List<Artist>>

    @Query("SELECT * FROM set_video_id WHERE videoId = :videoId")
    suspend fun getSetVideoId(videoId: String): SetVideoIdEntity?

    @Query("SELECT * FROM format WHERE id = :id")
    fun format(id: String?): Flow<FormatEntity?>

    @Query("SELECT * FROM lyrics WHERE id = :id")
    fun lyrics(id: String?): Flow<LyricsEntity?>

    @Upsert
    suspend fun upsert(map: SongAlbumMap)

    @Upsert
    suspend fun upsert(lyrics: LyricsEntity)

    @Upsert
    suspend fun upsert(format: FormatEntity)

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist
    """)
    fun getAllPlaylists(): Flow<List<Playlist>>

    
    @RawQuery
    suspend fun rawQuery(query: androidx.sqlite.db.SupportSQLiteQuery): Int

    

    suspend fun addSongToPlaylist(playlist: Playlist, songIds: List<String>) {
        val playlistId = playlist.id
        
        val currentMaxPosition = (playlistSongMaps(playlistId, 0).maxOfOrNull { it.position } ?: -1) + 1
        songIds.forEachIndexed { index, songId ->
            insert(PlaylistSongMap(playlistId = playlistId, songId = songId, position = currentMaxPosition + index))
        }
    }

    suspend fun songs(sortType: SongSortType, descending: Boolean): Flow<List<Song>> {
        return when (sortType) {
            SongSortType.CREATE_DATE -> songsByCreateDateAsc()
            SongSortType.NAME -> songsByNameAsc()
            SongSortType.ARTIST -> songsByNameAsc()
            SongSortType.PLAY_TIME -> songsByPlayTimeAsc()
        }
    }

    suspend fun likedSongs(sortType: SongSortType, descending: Boolean): Flow<List<Song>> {
        return when (sortType) {
            SongSortType.CREATE_DATE -> likedSongsByCreateDateAsc()
            SongSortType.NAME -> likedSongsByNameAsc()
            SongSortType.ARTIST -> likedSongsByNameAsc()
            SongSortType.PLAY_TIME -> likedSongsByPlayTimeAsc()
        }
    }

    suspend fun artistSongs(artistId: String, sortType: ArtistSongSortType, descending: Boolean): Flow<List<Song>> {
        return when (sortType) {
            ArtistSongSortType.CREATE_DATE -> artistSongsByCreateDateAsc(artistId)
            ArtistSongSortType.NAME -> artistSongsByNameAsc(artistId)
            ArtistSongSortType.PLAY_TIME -> artistSongsByPlayTimeAsc(artistId)
        }
    }

    suspend fun artists(sortType: ArtistSortType, descending: Boolean): Flow<List<Artist>> {
        return when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsByCreateDateAsc()
            ArtistSortType.NAME -> artistsByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsByPlayTimeAsc()
        }
    }

    fun artistsBookmarked(sortType: ArtistSortType, descending: Boolean): Flow<List<Artist>> {
        return when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsBookmarkedByCreateDateAsc()
            ArtistSortType.NAME -> artistsBookmarkedByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBookmarkedBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsBookmarkedByPlayTimeAsc()
        }
    }

    suspend fun albums(sortType: AlbumSortType, descending: Boolean): Flow<List<Album>> {
        return when (sortType) {
            AlbumSortType.CREATE_DATE -> albumsByCreateDateAsc()
            AlbumSortType.NAME -> albumsByNameAsc()
            AlbumSortType.YEAR -> albumsByYearAsc()
            AlbumSortType.SONG_COUNT -> albumsBySongCountAsc()
            AlbumSortType.LENGTH -> albumsByLengthAsc()
            AlbumSortType.PLAY_TIME -> albumsByPlayTimeAsc()
            else -> albumsByCreateDateAsc()
        }
    }

    fun albumsLiked(sortType: AlbumSortType, descending: Boolean): Flow<List<Album>> {
        return when (sortType) {
            AlbumSortType.CREATE_DATE -> albumsLikedByCreateDateAsc()
            AlbumSortType.NAME -> albumsLikedByNameAsc()
            AlbumSortType.YEAR -> albumsLikedByYearAsc()
            AlbumSortType.SONG_COUNT -> albumsLikedBySongCountAsc()
            AlbumSortType.LENGTH -> albumsLikedByLengthAsc()
            AlbumSortType.PLAY_TIME -> albumsLikedByPlayTimeAsc()
            else -> albumsLikedByCreateDateAsc()
        }
    }

    suspend fun playlists(sortType: PlaylistSortType, descending: Boolean, ownerId: String, isLocalMode: Boolean): Flow<List<Playlist>> {
        return when (sortType) {
            PlaylistSortType.CREATE_DATE -> playlistsByCreateDateAsc(ownerId, isLocalMode)
            PlaylistSortType.LAST_UPDATED -> playlistsByUpdatedDateAsc(ownerId, isLocalMode)
            PlaylistSortType.NAME -> playlistsByNameAsc(ownerId, isLocalMode)
            PlaylistSortType.SONG_COUNT -> playlistsBySongCountAsc(ownerId, isLocalMode)
        }
    }

    suspend fun getLyrics(id: String): LyricsEntity? = lyrics(id).first()

    
    suspend fun insert(mediaMetadata: MediaMetadata) = insert(mediaMetadata) { it }

    suspend fun insert(mediaMetadata: MediaMetadata, block: (SongEntity) -> SongEntity) {
        val song = block(
            SongEntity(
                id = mediaMetadata.id,
                title = mediaMetadata.title,
                duration = mediaMetadata.duration,
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumId = mediaMetadata.album?.id,
                albumName = mediaMetadata.album?.title,
            )
        )
        insert(song)
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: ArtistEntity.generateArtistId()
            insert(ArtistEntity(id = artistId, name = artist.name))
            insert(SongArtistMap(songId = song.id, artistId = artistId, position = index))
        }
        mediaMetadata.album?.let { album ->
            insert(AlbumEntity(id = album.id, title = album.title, songCount = 0, duration = 0))
            insert(SongAlbumMap(songId = song.id, albumId = album.id))
        }
    }

    suspend fun insert(albumPage: AlbumPage) {
        val albumId = albumPage.album.browseId
        insert(
            AlbumEntity(
                id = albumId,
                title = albumPage.album.title,
                year = albumPage.album.year,
                thumbnailUrl = albumPage.album.thumbnail,
                songCount = albumPage.songs.size,
                duration = albumPage.songs.sumOf { it.duration ?: 0 },
            )
        )
        albumPage.songs.forEachIndexed { index, song ->
            val songEntity = SongEntity(
                id = song.id,
                title = song.title,
                duration = song.duration ?: -1,
                thumbnailUrl = song.thumbnail,
                albumId = albumId,
                albumName = albumPage.album.title,
            )
            insert(songEntity)
            insert(SongAlbumMap(songId = song.id, albumId = albumId, index = index))
            song.artists?.forEachIndexed { artistIndex, artist ->
                val artistId = artist.id ?: ArtistEntity.generateArtistId()
                insert(ArtistEntity(id = artistId, name = artist.name))
                insert(SongArtistMap(songId = song.id, artistId = artistId, position = artistIndex))
            }
        }
    }

    suspend fun update(song: Song, mediaMetadata: MediaMetadata) {
        update(
            song.song.copy(
                title = mediaMetadata.title,
                duration = mediaMetadata.duration,
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumId = mediaMetadata.album?.id,
                albumName = mediaMetadata.album?.title,
            )
        )
    }

    suspend fun update(album: AlbumEntity, albumPage: AlbumPage, artists: List<ArtistEntity>) {
        update(
            album.copy(
                title = albumPage.album.title,
                year = albumPage.album.year,
                thumbnailUrl = albumPage.album.thumbnail,
                songCount = albumPage.songs.size,
                duration = albumPage.songs.sumOf { it.duration ?: 0 },
            )
        )
        artists.forEach { insert(it) }
        artists.forEachIndexed { index, artist ->
            insert(AlbumArtistMap(albumId = album.id, artistId = artist.id, order = index))
        }
    }

    suspend fun update(artist: ArtistEntity, artistPage: ArtistPage) {
        update(
            artist.copy(
                name = artistPage.artist.title,
                thumbnailUrl = artistPage.artist.thumbnail,
                lastUpdateTime = LocalDateTime.now(),
            )
        )
    }

    suspend fun update(playlistEntity: PlaylistEntity, playlistItem: PlaylistItem) {
        update(
            playlistEntity.copy(
                name = playlistItem.title ?: playlistEntity.name,
                browseId = playlistItem.id,
                remoteSongCount = playlistItem.songCountText?.let {
                    it.filter { c -> c.isDigit() }.toIntOrNull()
                },
                lastUpdateTime = LocalDateTime.now(),
            )
        )
    }

    suspend fun checkpoint() {
        
    }
}