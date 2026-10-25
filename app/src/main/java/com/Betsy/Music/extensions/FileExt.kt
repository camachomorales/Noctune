/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.extensions

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

operator fun File.div(child: String): File = File(this, child)

fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)

fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)


