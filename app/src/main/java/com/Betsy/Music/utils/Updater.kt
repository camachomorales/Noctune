/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.utils

   
                                                                                             
                                                                                             
                       
   
object Updater {
    var lastCheckTime = -1L
        private set

    @Suppress("RedundantSuspendModifier")
    suspend fun getLatestVersionName(): Result<String> =
        Result.failure(UnsupportedOperationException("Actualizaciones gestionadas por la tienda"))
}
