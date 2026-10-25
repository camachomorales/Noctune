/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        if (value != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)
        } else {
            null
        }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? =
        date?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
}


