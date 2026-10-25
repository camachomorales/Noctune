/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius.compat

import com.betsy.audius.AudiusClient
import com.betsy.audius.compat.models.*
import com.betsy.audius.compat.pages.*
import com.betsy.audius.models.AudiusArtist
import com.betsy.audius.models.AudiusPlaylist
import com.betsy.audius.models.AudiusSong
import com.betsy.audius.models.AudiusStream
import com.betsy.audius.models.TrendingTime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.Proxy

   
                                                                                                
  
                     
                                           
                                                                     
                                                                                                 
                                                                                                          
                                                  
   
object AudiusApi {

    

    @Volatile
    private var client: AudiusClient = AudiusClient(appName = "BetsyMusic")

                                                                                                  
    fun init(appName: String, apiKey: String?) {
        client = AudiusClient(appName = appName, apiKeyOrToken = apiKey?.takeIf { it.isNotBlank() })
    }

    val api: AudiusClient get() = client

                                                                                   
    const val TRENDING_ID = "audius:trending"

    private const val PAGE = 25
    private const val SEP = "\u001F"
    private const val ARTIST_PREFIX = "artist:"

    

    var locale: StreamingLocale = StreamingLocale(gl = "US", hl = "en")
    var proxy: Proxy? = null

    suspend fun accountInfo(): Result<AccountInfo> = unsupported("Audius no tiene cuentas de usuario")

    

    private fun AudiusArtist.ref() = Artist(name = name, id = id.ifBlank { null })

    private fun AudiusSong.toSongItem(album: Album? = null) = SongItem(
        id = id,
        title = title,
        artists = listOf(artist.ref()),
        album = album,
        duration = durationSeconds,
        thumbnail = artworkUrl ?: artist.avatarUrl.orEmpty(),
        explicit = false,
        endpoint = WatchEndpoint(videoId = id),
        webUrl = url,
        isStreamable = isStreamable,
        isDownloadable = isDownloadable,
    )

    private fun AudiusPlaylist.toAlbumItem() = AlbumItem(
        browseId = id,
        playlistId = id,
        title = title,
        artists = listOf(owner.ref()),
        year = null,
        thumbnail = artworkUrl.orEmpty(),
        webUrl = url,
    )

    private fun AudiusPlaylist.toPlaylistItem() = PlaylistItem(
        id = id,
        title = title,
        author = owner.ref(),
        songCountText = "$trackCount canciones",
        thumbnail = artworkUrl.orEmpty(),
        playEndpoint = WatchEndpoint(playlistId = id),
        shuffleEndpoint = WatchEndpoint(playlistId = id, params = "shuffle"),
        radioEndpoint = WatchEndpoint(playlistId = id, params = "radio"),
        isEditable = false,
        webUrl = url,
    )

    private fun AudiusPlaylist.toYTItem(): YTItem = if (isAlbum) toAlbumItem() else toPlaylistItem()

    private fun AudiusArtist.toArtistItem() = ArtistItem(
        id = id,
        title = name,
        thumbnail = avatarUrl.orEmpty(),
        channelId = null, 
        shuffleEndpoint = WatchEndpoint(playlistId = "$ARTIST_PREFIX$id", params = "shuffle"),
        radioEndpoint = WatchEndpoint(playlistId = "$ARTIST_PREFIX$id", params = "radio"),
        subscriberCountText = followerCount?.let { "$it seguidores" },
        webUrl = profileUrl,
    )

    private fun <T> unsupported(what: String): Result<T> =
        Result.failure(UnsupportedOperationException(what))

    

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("songs")
            val FILTER_VIDEO = SearchFilter("videos") 
            val FILTER_ALBUM = SearchFilter("albums")
            val FILTER_ARTIST = SearchFilter("artists")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("featured_playlists")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("community_playlists")
        }
    }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> = runCatching {
        if (query.isBlank()) return@runCatching SearchSuggestions(emptyList(), emptyList())
        coroutineScope {
            val songs = async { api.searchSongs(query, limit = 6).getOrDefault(emptyList()) }
            val artists = async { api.searchArtists(query, limit = 3).getOrDefault(emptyList()) }
            val s = songs.await()
            val a = artists.await()
            SearchSuggestions(
                queries = (s.map { it.title } + a.map { it.name }).distinct().take(8),
                recommendedItems = s.take(3).map { it.toSongItem() } + a.take(2).map { it.toArtistItem() },
            )
        }
    }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> = runCatching {
        val results = api.search(query, limit = 10).getOrThrow()
        val summaries = buildList {
            if (results.songs.isNotEmpty())
                add(SearchSummary("Canciones", results.songs.map { it.toSongItem() }))
            if (results.artists.isNotEmpty())
                add(SearchSummary("Artistas", results.artists.map { it.toArtistItem() }))
            results.playlists.filter { it.isAlbum }.takeIf { it.isNotEmpty() }
                ?.let { add(SearchSummary("Álbumes", it.map { p -> p.toAlbumItem() })) }
            results.playlists.filter { !it.isAlbum }.takeIf { it.isNotEmpty() }
                ?.let { add(SearchSummary("Listas", it.map { p -> p.toPlaylistItem() })) }
        }
        SearchSummaryPage(summaries)
    }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> =
        searchPage(query, filter.value, 0)

    suspend fun searchContinuation(continuation: String): Result<SearchResult> {
        val parts = continuation.split(SEP, limit = 3)
        if (parts.size < 3) return unsupported("Continuación inválida")
        val offset = parts[0].toIntOrNull() ?: return unsupported("Continuación inválida")
        return searchPage(query = parts[2], kind = parts[1], offset = offset)
    }

    private suspend fun searchPage(query: String, kind: String, offset: Int): Result<SearchResult> = runCatching {
        val rawCount: Int
        val items: List<YTItem>
        when (kind) {
            SearchFilter.FILTER_ARTIST.value -> {
                val r = api.searchArtists(query, PAGE, offset).getOrThrow()
                rawCount = r.size
                items = r.map { it.toArtistItem() }
            }
            SearchFilter.FILTER_ALBUM.value,
            SearchFilter.FILTER_FEATURED_PLAYLIST.value,
            SearchFilter.FILTER_COMMUNITY_PLAYLIST.value -> {
                val r = api.searchPlaylists(query, PAGE, offset).getOrThrow()
                rawCount = r.size
                val wantAlbums = kind == SearchFilter.FILTER_ALBUM.value
                items = r.filter { it.isAlbum == wantAlbums }.map { it.toYTItem() }
            }
            else -> {
                val r = api.searchSongs(query, PAGE, offset).getOrThrow()
                rawCount = r.size
                items = r.filter { it.isStreamable }.map { it.toSongItem() }
            }
        }
        SearchResult(
            items = items,
            continuation = if (rawCount >= PAGE) listOf(offset + PAGE, kind, query).joinToString(SEP) else null,
        )
    }

    

    suspend fun album(browseId: String, withSongs: Boolean = true): Result<AlbumPage> = runCatching {
        coroutineScope {
            val meta = async { api.playlist(browseId).getOrThrow() }
            val songs = async { if (withSongs) api.playlistSongs(browseId).getOrThrow() else emptyList() }
            val album = meta.await()
            val ref = Album(album.title, album.id)
            AlbumPage(
                album = album.toAlbumItem(),
                songs = songs.await().filter { it.isStreamable }.map { it.toSongItem(ref) },
                otherVersions = emptyList(),
            )
        }
    }

    suspend fun albumSongs(playlistId: String): Result<List<SongItem>> = runCatching {
        album(playlistId).getOrThrow().songs
    }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        if (playlistId == TRENDING_ID) {
            val songs = api.trendingSongs(TrendingTime.WEEK, limit = 50).getOrThrow()
            return@runCatching PlaylistPage(
                playlist = PlaylistItem(
                    id = TRENDING_ID,
                    title = "Tendencias de la semana",
                    author = Artist("Audius", null),
                    songCountText = "${songs.size} canciones",
                    thumbnail = songs.firstOrNull()?.artworkUrl.orEmpty(),
                    playEndpoint = WatchEndpoint(playlistId = TRENDING_ID),
                    shuffleEndpoint = WatchEndpoint(playlistId = TRENDING_ID, params = "shuffle"),
                    radioEndpoint = null,
                    webUrl = "https://audius.co/trending",
                ),
                songs = songs.filter { it.isStreamable }.map { it.toSongItem() },
                songsContinuation = null,
                continuation = null,
            )
        }
        coroutineScope {
            val meta = async { api.playlist(playlistId).getOrThrow() }
            val songs = async { api.playlistSongs(playlistId).getOrThrow() }
            val p = meta.await()
            val ref = Album(p.title, p.id)
            PlaylistPage(
                playlist = p.toPlaylistItem(),
                songs = songs.await().filter { it.isStreamable }.map { it.toSongItem(ref) },
                songsContinuation = null,
                continuation = null,
            )
        }
    }

                                                                           
    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        Result.success(PlaylistContinuationPage(emptyList(), null))

    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        coroutineScope {
            val info = async { api.artist(browseId).getOrThrow() }
            val songs = async { api.artistSongs(browseId, limit = 20).getOrDefault(emptyList()) }
            val albums = async { api.artistAlbums(browseId, limit = 20).getOrDefault(emptyList()) }
            val lists = async { api.artistPlaylists(browseId, limit = 20).getOrDefault(emptyList()) }
            val a = info.await()
            val sections = buildList {
                songs.await().filter { it.isStreamable }.takeIf { it.isNotEmpty() }?.let {
                    add(ArtistSection("Canciones", it.map { s -> s.toSongItem() }, BrowseEndpoint(browseId, "songs", BrowseEndpoint.PAGE_ARTIST)))
                }
                albums.await().filter { it.isAlbum }.takeIf { it.isNotEmpty() }?.let {
                    add(ArtistSection("Álbumes", it.map { p -> p.toAlbumItem() }, BrowseEndpoint(browseId, "albums", BrowseEndpoint.PAGE_ARTIST)))
                }
                lists.await().filter { !it.isAlbum }.takeIf { it.isNotEmpty() }?.let {
                    add(ArtistSection("Listas", it.map { p -> p.toPlaylistItem() }, BrowseEndpoint(browseId, "playlists", BrowseEndpoint.PAGE_ARTIST)))
                }
            }
            ArtistPage(artist = a.toArtistItem(), sections = sections, description = a.bio)
        }
    }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val (title, items, hasMore) = artistItemsChunk(endpoint.browseId, endpoint.params ?: "songs", 0)
        ArtistItemsPage(
            title = title,
            items = items,
            continuation = if (hasMore) listOf(PAGE * 2, endpoint.params ?: "songs", endpoint.browseId).joinToString(SEP) else null,
        )
    }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> = runCatching {
        val parts = continuation.split(SEP, limit = 3)
        require(parts.size == 3) { "Continuación inválida" }
        val offset = parts[0].toInt()
        val (_, items, hasMore) = artistItemsChunk(parts[2], parts[1], offset)
        ArtistItemsContinuationPage(
            items = items,
            continuation = if (hasMore) listOf(offset + PAGE * 2, parts[1], parts[2]).joinToString(SEP) else null,
        )
    }

    private data class Chunk(val title: String, val items: List<YTItem>, val hasMore: Boolean)

    private suspend fun artistItemsChunk(artistId: String, kind: String, offset: Int): Chunk {
        val size = PAGE * 2
        return when (kind) {
            "albums" -> {
                val r = api.artistAlbums(artistId, limit = size).getOrThrow().filter { it.isAlbum }
                Chunk("Álbumes", r.map { it.toAlbumItem() }, false)
            }
            "playlists" -> {
                val r = api.artistPlaylists(artistId, limit = size).getOrThrow().filter { !it.isAlbum }
                Chunk("Listas", r.map { it.toPlaylistItem() }, false)
            }
            else -> {
                val r = api.artistSongs(artistId, limit = size, offset = offset).getOrThrow()
                Chunk("Canciones", r.filter { it.isStreamable }.map { it.toSongItem() }, r.size >= size)
            }
        }
    }

    

    private val GENRES = listOf(
        "Electronic", "Hip-Hop/Rap", "Pop", "Rock", "Lo-Fi", "Latin", "R&B/Soul", "Alternative",
        "Jazz", "Ambient", "Metal", "Folk", "Funk", "Reggae", "Classical", "Country", "Blues",
        "House", "Techno", "Trap", "Dubstep", "Soundtrack", "World", "Acoustic",
    )

    private val GENRE_COLORS = longArrayOf(
        0xFF00D9FF, 0xFFFF6B6B, 0xFFFFD166, 0xFF06D6A0, 0xFFB388FF, 0xFFFF9F1C,
        0xFF4ECDC4, 0xFFEF476F, 0xFF118AB2, 0xFF8AC926,
    )

    private fun genreItems() = GENRES.mapIndexed { i, g ->
        MoodAndGenres.Item(
            title = g,
            stripeColor = GENRE_COLORS[i % GENRE_COLORS.size],
            endpoint = BrowseEndpoint(browseId = "genre", params = "genre:$g"),
        )
    }

    suspend fun home(): Result<HomePage> = runCatching {
        coroutineScope {
            val week = async { api.trendingSongs(TrendingTime.WEEK, limit = 20) }
            val lists = async { api.trendingPlaylists(TrendingTime.WEEK, limit = 30) }
            val genres = listOf("Electronic", "Hip-Hop/Rap", "Lo-Fi", "Pop").map { g ->
                g to async { api.trendingSongs(TrendingTime.MONTH, genre = g, limit = 15) }
            }

            val sections = mutableListOf<HomePage.Section>()
            val weekRes = week.await()
            weekRes.getOrNull()?.filter { it.isStreamable }?.takeIf { it.isNotEmpty() }?.let {
                sections += HomePage.Section(
                    title = "Tendencias de la semana",
                    label = "Audius",
                    thumbnail = null,
                    endpoint = BrowseEndpoint("trending", "trending:week"),
                    items = it.map { s -> s.toSongItem() },
                )
            }
            val pl = lists.await().getOrDefault(emptyList())
            pl.filter { it.isAlbum }.takeIf { it.isNotEmpty() }?.let {
                sections += HomePage.Section("Álbumes del momento", null, null, null, it.map { p -> p.toAlbumItem() })
            }
            pl.filter { !it.isAlbum }.takeIf { it.isNotEmpty() }?.let {
                sections += HomePage.Section("Listas para descubrir", null, null, null, it.map { p -> p.toPlaylistItem() })
            }
            genres.forEach { (g, deferred) ->
                deferred.await().getOrNull()?.filter { it.isStreamable }?.takeIf { it.isNotEmpty() }?.let {
                    sections += HomePage.Section(
                        title = "En $g",
                        label = "Género",
                        thumbnail = null,
                        endpoint = BrowseEndpoint("genre", "genre:$g"),
                        items = it.map { s -> s.toSongItem() },
                    )
                }
            }
            if (sections.isEmpty()) throw weekRes.exceptionOrNull() ?: IllegalStateException("Audius no devolvió contenido")
            HomePage(sections)
        }
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        ExplorePage(newReleaseAlbums = newReleaseAlbums().getOrDefault(emptyList()), moodAndGenres = genreItems())
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        api.trendingPlaylists(TrendingTime.WEEK, limit = 50).getOrThrow()
            .filter { it.isAlbum }
            .map { it.toAlbumItem() }
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        Result.success(listOf(MoodAndGenres(title = "Géneros", items = genreItems())))

    suspend fun browse(browseId: String, params: String?): Result<BrowseResult> = runCatching {
        when {
            params?.startsWith("genre:") == true -> {
                val g = params.removePrefix("genre:")
                val month = api.trendingSongs(TrendingTime.MONTH, genre = g, limit = 40).getOrThrow()
                val week = api.trendingSongs(TrendingTime.WEEK, genre = g, limit = 20).getOrDefault(emptyList())
                BrowseResult(
                    title = g,
                    items = listOf(
                        BrowseResult.Item("Tendencias de la semana", week.filter { it.isStreamable }.map { it.toSongItem() }),
                        BrowseResult.Item("Tendencias del mes", month.filter { it.isStreamable }.map { it.toSongItem() }),
                    ).filter { it.items.isNotEmpty() },
                )
            }
            params?.startsWith("trending:") == true -> {
                val time = when (params.removePrefix("trending:")) {
                    "month" -> TrendingTime.MONTH
                    "year" -> TrendingTime.YEAR
                    "all" -> TrendingTime.ALL_TIME
                    else -> TrendingTime.WEEK
                }
                val r = api.trendingSongs(time, limit = 50).getOrThrow().filter { it.isStreamable }
                BrowseResult("Tendencias", listOf(BrowseResult.Item(null, r.map { it.toSongItem() })))
            }
            else -> error("Contenido no disponible en Audius")
        }
    }

    

       
                                                          
                                                                                                            
       
    private suspend fun radioFor(song: AudiusSong, exclude: Set<String> = emptySet(), size: Int = 25): List<SongItem> {
        val skip = exclude + song.id
        val pool = api.relatedSongs(song.id, size + skip.size).getOrNull()?.takeIf { it.size > 1 }
            ?: song.genre?.takeIf { it.isNotBlank() }?.let {
                api.trendingSongs(TrendingTime.MONTH, genre = it, limit = size + skip.size).getOrNull()?.shuffled()
            }
            ?: api.artistSongs(song.artist.id, limit = size + skip.size).getOrNull()
            ?: emptyList()
        return pool.filter { it.isStreamable && it.id !in skip }.distinctBy { it.id }.take(size).map { it.toSongItem() }
    }

    private suspend fun songsOfPlaylistId(playlistId: String): Pair<String?, List<SongItem>> = when {
        playlistId == TRENDING_ID -> {
            val p = playlist(TRENDING_ID).getOrThrow()
            p.playlist.title to p.songs
        }
        playlistId.startsWith(ARTIST_PREFIX) -> {
            val artistId = playlistId.removePrefix(ARTIST_PREFIX)
            val a = api.artist(artistId).getOrNull()
            a?.name to api.artistSongs(artistId, limit = 50).getOrThrow().filter { it.isStreamable }.map { it.toSongItem() }
        }
        else -> {
            val p = album(playlistId).getOrThrow()
            p.album.title to p.songs
        }
    }

    suspend fun next(endpoint: WatchEndpoint, continuation: String? = null): Result<NextResult> = runCatching {
        
        if (continuation != null) {
            return@runCatching NextResult(items = emptyList(), continuation = null, endpoint = endpoint)
        }
        val videoId = endpoint.videoId
        val playlistId = endpoint.playlistId

        if (playlistId != null) {
            var (title, songs) = songsOfPlaylistId(playlistId)
            if (endpoint.params == "shuffle") songs = songs.shuffled()
            val wantsRadio = endpoint.params == "radio" || endpoint.params == "wAEB"
            val extra = if (wantsRadio && songs.isNotEmpty()) {
                val seed = api.song(songs.first().id).getOrNull()
                if (seed != null) radioFor(seed, exclude = songs.map { it.id }.toSet()) else emptyList()
            } else emptyList()
            val items = songs + extra
            val index = videoId?.let { id -> items.indexOfFirst { it.id == id }.takeIf { it >= 0 } } ?: 0
            return@runCatching NextResult(
                title = title,
                items = items,
                currentIndex = index,
                relatedEndpoint = songs.firstOrNull()?.let { BrowseEndpoint(it.id, "related") },
                continuation = null,
                endpoint = endpoint,
            )
        }

        requireNotNull(videoId) { "WatchEndpoint sin videoId ni playlistId" }
        val current = api.song(videoId).getOrThrow()
        val items = listOf(current.toSongItem()) + radioFor(current)
        NextResult(
            items = items,
            currentIndex = 0,
            relatedEndpoint = BrowseEndpoint(videoId, "related"),
            continuation = null,
            endpoint = endpoint,
        )
    }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> = runCatching {
        val current = api.song(endpoint.browseId).getOrThrow()
        RelatedPage(
            songs = radioFor(current),
            albums = emptyList(),
            artists = listOf(current.artist.toArtistItem()),
            playlists = emptyList(),
        )
    }

    suspend fun queue(videoIds: List<String>? = null, playlistId: String? = null): Result<List<SongItem>> = runCatching {
        when {
            videoIds != null -> api.songs(videoIds).getOrThrow().map { it.toSongItem() }
            playlistId != null -> songsOfPlaylistId(playlistId).second
            else -> emptyList()
        }
    }

                                                                                     
    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> = Result.success(null)

    suspend fun transcript(videoId: String): Result<String> = unsupported("Audius no ofrece letras")

                                                                              
    suspend fun stream(songId: String): Result<AudiusStream> = api.resolveStream(songId)

                                                                        
    suspend fun registerPlayback(playlistId: String? = null, playbackTracking: String = ""): Result<Unit> =
        Result.success(Unit)

    

    suspend fun library(browseId: String, tabIndex: Int = 0): Result<LibraryPage> = unsupported("Sin biblioteca remota")
    suspend fun libraryContinuation(continuation: String): Result<LibraryContinuationPage> = unsupported("Sin biblioteca remota")
    suspend fun libraryRecentActivity(): Result<LibraryPage> = unsupported("Sin biblioteca remota")
    suspend fun musicHistory(): Result<HistoryPage> = unsupported("Sin historial remoto")
    suspend fun likeVideo(videoId: String, like: Boolean): Result<Unit> = unsupported("Sin cuenta")
    suspend fun likePlaylist(playlistId: String, like: Boolean): Result<Unit> = unsupported("Sin cuenta")
    suspend fun subscribeChannel(channelId: String, subscribe: Boolean): Result<Unit> = unsupported("Sin cuenta")
    suspend fun getChannelId(browseId: String): String = browseId
    suspend fun addToPlaylist(playlistId: String, videoId: String): Result<Unit> = unsupported("Listas remotas no soportadas")
    suspend fun addPlaylistToPlaylist(playlistId: String, addPlaylistId: String): Result<Unit> = unsupported("Listas remotas no soportadas")
    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String): Result<Unit> = unsupported("Listas remotas no soportadas")
    suspend fun moveSongPlaylist(playlistId: String, setVideoId: String, successorSetVideoId: String): Result<Unit> = unsupported("Listas remotas no soportadas")
    suspend fun createPlaylist(title: String): Result<String?> = Result.success(null)
    suspend fun renamePlaylist(playlistId: String, name: String): Result<Unit> = unsupported("Listas remotas no soportadas")
    suspend fun deletePlaylist(playlistId: String): Result<Unit> = unsupported("Listas remotas no soportadas")
}
