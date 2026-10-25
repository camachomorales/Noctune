/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.audius.compat.utils

import com.betsy.audius.compat.pages.LibraryPage
import com.betsy.audius.compat.pages.PlaylistPage

                                                                                
@Suppress("RedundantSuspendModifier")
suspend fun Result<PlaylistPage>.completed(): Result<PlaylistPage> = this

@Suppress("RedundantSuspendModifier")
suspend fun Result<LibraryPage>.completedLibraryPage(): Result<LibraryPage> = this

                                                                                                   
fun parseCookieString(cookie: String): Map<String, String> =
    cookie
        .split("; ")
        .filter { it.isNotEmpty() && it.contains("=") }
        .associate {
            val i = it.indexOf('=')
            it.substring(0, i) to it.substring(i + 1)
        }
