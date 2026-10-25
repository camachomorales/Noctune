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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import java.util.concurrent.ExecutionException

class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: error("Could not decode image data")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        scope.future(Dispatchers.IO) {
            val result =
                context.imageLoader.execute(
                    ImageRequest
                        .Builder(context)
                        .data(uri)
                        .allowHardware(false)
                        .build(),
                )
            if (result is ErrorResult) {
                throw ExecutionException(result.throwable)
            }
            try {
                (result.drawable as BitmapDrawable).bitmap
            } catch (e: Exception) {
                throw ExecutionException(e)
            }
        }
}


