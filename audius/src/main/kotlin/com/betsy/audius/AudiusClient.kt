/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius

import com.betsy.audius.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.contentLength
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

   
                                            
  
                                                            
                                                                                              
                                                                  
   
class AudiusClient(
    private val appName: String,
    private val apiKeyOrToken: String? = null,
    private val baseUrl: String = "https://api.audius.co/v1",
) {
    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

                                                                                          
    private val noRedirect = HttpClient(OkHttp) {
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    private suspend inline fun <reified T> get(
        path: String,
        vararg params: Pair<String, Any?>,
    ): T {
        val response = http.get("$baseUrl$path") {
            url {
                parameters.append("app_name", appName)
                params.forEach { (key, value) ->
                    if (value != null) parameters.append(key, value.toString())
                }
            }
            apiKeyOrToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        return response.body<AudiusResponse<T>>().data
            ?: error("Respuesta vacía de Audius en $path")
    }

    

    suspend fun searchSongs(query: String, limit: Int = 25, offset: Int = 0) = runCatching {
        get<List<TrackDto>>("/tracks/search", "query" to query, "limit" to limit, "offset" to offset)
            .map { it.toSong() }
    }

    suspend fun searchArtists(query: String, limit: Int = 25, offset: Int = 0) = runCatching {
        get<List<UserDto>>("/users/search", "query" to query, "limit" to limit, "offset" to offset)
            .map { it.toArtist() }
    }

    suspend fun searchPlaylists(query: String, limit: Int = 25, offset: Int = 0) = runCatching {
        get<List<PlaylistDto>>("/playlists/search", "query" to query, "limit" to limit, "offset" to offset)
            .map { it.toPlaylist() }
    }

                                                                                              
    suspend fun search(query: String, limit: Int = 15): Result<AudiusSearchResults> = runCatching {
        coroutineScope {
            val songs = async { searchSongs(query, limit) }
            val artists = async { searchArtists(query, limit) }
            val playlists = async { searchPlaylists(query, limit) }
            AudiusSearchResults(
                songs = songs.await().getOrThrow(),
                artists = artists.await().getOrDefault(emptyList()),
                playlists = playlists.await().getOrDefault(emptyList()),
            )
        }
    }

    

    suspend fun trendingSongs(
        time: TrendingTime = TrendingTime.WEEK,
        genre: String? = null,
        limit: Int = 25,
    ) = runCatching {
        get<List<TrackDto>>("/tracks/trending", "time" to time.value, "genre" to genre, "limit" to limit)
            .map { it.toSong() }
    }

    suspend fun trendingPlaylists(
        time: TrendingTime = TrendingTime.WEEK,
        limit: Int = 25,
    ) = runCatching {
        get<List<PlaylistDto>>("/playlists/trending", "time" to time.value, "limit" to limit)
            .map { it.toPlaylist() }
    }

    

    suspend fun song(id: String) = runCatching {
        get<TrackDto>("/tracks/$id").toSong()
    }

    suspend fun artist(id: String) = runCatching {
        get<UserDto>("/users/$id").toArtist()
    }

    suspend fun artistSongs(id: String, limit: Int = 50, offset: Int = 0) = runCatching {
        get<List<TrackDto>>("/users/$id/tracks", "limit" to limit, "offset" to offset)
            .map { it.toSong() }
    }

    suspend fun playlist(id: String) = runCatching {
        
        get<List<PlaylistDto>>("/playlists/$id").first().toPlaylist()
    }

    suspend fun playlistSongs(id: String) = runCatching {
        get<List<TrackDto>>("/playlists/$id/tracks").map { it.toSong() }
    }

    suspend fun songs(ids: List<String>) = runCatching {
        coroutineScope {
            ids.distinct()
                .map { id -> async { get<TrackDto>("/tracks/$id").toSong() } }
                .map { it.await() }
        }
    }

                                                                                            
    suspend fun relatedSongs(id: String, limit: Int = 25) = runCatching {
        get<List<TrackDto>>("/tracks/$id/related", "limit" to limit).map { it.toSong() }
    }

    suspend fun artistAlbums(id: String, limit: Int = 25) = runCatching {
        get<List<PlaylistDto>>("/users/$id/albums", "limit" to limit).map { it.toPlaylist() }
    }

    suspend fun artistPlaylists(id: String, limit: Int = 25) = runCatching {
        get<List<PlaylistDto>>("/users/$id/playlists", "limit" to limit).map { it.toPlaylist() }
    }

    

       
                                                                                  
                                                                             
       
    fun streamUrl(songId: String): String =
        "$baseUrl/tracks/$songId/stream?app_name=$appName"

       
                                                                              
                                                                    
       
    suspend fun resolveStream(songId: String): Result<AudiusStream> = runCatching {
        val streamEndpoint = "$baseUrl/tracks/$songId/stream"
        val (status, location) = noRedirect.prepareGet(streamEndpoint) {
            url { parameters.append("app_name", appName) }
            apiKeyOrToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }.execute { r: HttpResponse -> r.status.value to r.headers[HttpHeaders.Location] }

        val finalUrl = when (status) {
            in 300..399 -> location ?: error("Audius redirigió sin Location")
            in 200..299 -> streamUrl(songId)
            401, 403 -> error("Audius rechazó la petición ($status). ¿Falta o es inválida la API key?")
            404 -> error("Canción no disponible en Audius")
            else -> error("Audius respondió $status")
        }

        val length = runCatching { http.head(finalUrl).contentLength() }.getOrNull()
        AudiusStream(url = finalUrl, contentLength = length?.takeIf { it > 0 })
    }
}
