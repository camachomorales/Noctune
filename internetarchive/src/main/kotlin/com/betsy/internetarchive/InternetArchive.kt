/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.internetarchive

   
                                                                
  
                                                                               
                                                                                         
   
object InternetArchive {
    val client: InternetArchiveClient by lazy { InternetArchiveClient() }

    fun isIaId(mediaId: String?): Boolean = mediaId?.startsWith(InternetArchiveClient.ID_PREFIX) == true

    fun parseId(mediaId: String?): IaTrackRef? {
        if (!isIaId(mediaId)) return null
        val rest = mediaId!!.removePrefix(InternetArchiveClient.ID_PREFIX)
        val slash = rest.indexOf('/')
        if (slash <= 0 || slash == rest.lastIndex) return null
        return IaTrackRef(identifier = rest.substring(0, slash), fileName = rest.substring(slash + 1))
    }

    fun mimeTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "mp3" -> "audio/mpeg"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/opus"
        "flac" -> "audio/flac"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        else -> "audio/mpeg"
    }

    fun codecFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "ogg", "oga" -> "vorbis"
        "opus" -> "opus"
        "flac" -> "flac"
        "m4a", "aac" -> "aac"
        "wav" -> "pcm"
        else -> "mp3"
    }
}
