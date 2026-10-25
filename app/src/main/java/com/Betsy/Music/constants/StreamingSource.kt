/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.constants

import androidx.datastore.preferences.core.stringPreferencesKey

                                                     
val StreamingSourceKey = stringPreferencesKey("streamingSource")

enum class StreamingSource {
    AUDIUS,
    INTERNET_ARCHIVE,
}
