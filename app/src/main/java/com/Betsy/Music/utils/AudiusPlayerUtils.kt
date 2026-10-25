/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.betsy.audius.compat.AudiusApi
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.constants.AudioQuality
import timber.log.Timber

   
                                                                    
                                                                              
  
                                                                          
                                                                      
   
object AudiusPlayerUtils {
    private const val TAG = "AudiusPlayerUtils"

                                                                                   
    private const val STREAM_TTL_SECONDS = 30 * 60

    data class AudioFormat(
        val itag: Int,
        val mimeType: String,
        val codecs: String,
        val bitrate: Int,
        val sampleRate: Int?,
        val contentLength: Long,
    )

    data class PlaybackData(
        val format: AudioFormat,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        val durationSeconds: Int?,
    )

       
                                                                                           
                                                            
       
    @Suppress("UNUSED_PARAMETER")
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        forDownload: Boolean = false,
    ): Result<PlaybackData> = runCatching {
        
        InternetArchive.parseId(videoId)?.let { ref ->
            return@runCatching PlaybackData(
                format = AudioFormat(
                    itag = 0,
                    mimeType = InternetArchive.mimeTypeFor(ref.fileName),
                    codecs = InternetArchive.codecFor(ref.fileName),
                    bitrate = 0,
                    sampleRate = null,
                    contentLength = 0L,
                ),
                streamUrl = InternetArchive.client.downloadUrl(ref.identifier, ref.fileName),
                streamExpiresInSeconds = 6 * 60 * 60,
                durationSeconds = null,
            )
        }

        val song = AudiusApi.api.song(videoId).getOrThrow()

        if (!song.isStreamable) {
            throw PlaybackException(
                "Esta canción no está disponible para streaming en Audius",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR,
            )
        }
        if (forDownload && !song.isDownloadable) {
            throw PlaybackException(
                "El artista no permite descargar esta canción",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR,
            )
        }

        val stream = AudiusApi.stream(videoId).getOrThrow()
        Timber.tag(TAG).d("Stream de Audius resuelto para $videoId")

        PlaybackData(
            format = AudioFormat(
                itag = 0,
                mimeType = stream.mimeType,
                codecs = "mp3",
                bitrate = stream.bitrate,
                sampleRate = stream.sampleRate,
                contentLength = stream.contentLength ?: 0L,
            ),
            streamUrl = stream.url,
            streamExpiresInSeconds = STREAM_TTL_SECONDS,
            durationSeconds = song.durationSeconds,
        )
    }

                                                                              
    suspend fun durationFor(videoId: String): Result<Int?> = runCatching {
        if (InternetArchive.isIaId(videoId)) return@runCatching (null as Int?)
        AudiusApi.api.song(videoId).getOrThrow().durationSeconds
    }
}
