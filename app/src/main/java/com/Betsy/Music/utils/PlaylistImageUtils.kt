/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

                                                        
fun saveCustomPlaylistImage(context: Context, playlistId: String, imageUri: Uri) {
    try {
        
        val playlistImagesDir = File(context.filesDir, "playlist_images")
        if (!playlistImagesDir.exists()) {
            playlistImagesDir.mkdirs()
        }

        
        val imageFile = File(playlistImagesDir, "playlist_${playlistId}.jpg")

        
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(imageFile).use { output ->
                input.copyTo(output)
            }
        }

        
        context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE).edit {
            putString("playlist_$playlistId", imageFile.toUri().toString())
        }
    } catch (e: IOException) {
        Timber.e(e, "Error al guardar imagen de playlist")
    }
}

                                                                  
fun getPlaylistImageUri(context: Context, playlistId: String): Uri? {
    val uriString = context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE)
        .getString("playlist_$playlistId", null)

    return if (uriString != null) {
        val uri = uriString.toUri()
        
        val file = File(uri.path ?: "")
        if (file.exists()) uri else null
    } else null
}

                                                      
fun deletePlaylistImage(context: Context, playlistId: String) {
    val uriString = context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE)
        .getString("playlist_$playlistId", null)

    if (uriString != null) {
        val file = File(uriString.toUri().path ?: "")
        if (file.exists()) {
            file.delete()
        }

        
        context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE).edit {
            remove("playlist_$playlistId")
        }
    }
}


