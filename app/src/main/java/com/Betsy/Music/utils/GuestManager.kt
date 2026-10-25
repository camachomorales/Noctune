/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.betsy.music.constants.GuestUserIdKey
import kotlinx.coroutines.flow.first
import java.util.UUID

object GuestManager {

       
                                                      
                                                     
                                                                
       
    suspend fun getOrCreateGuestId(dataStore: DataStore<Preferences>): String {
        val existing = dataStore.data.first()[GuestUserIdKey]
        if (!existing.isNullOrBlank()) return existing

        val newId = "guest_${UUID.randomUUID()}"
        dataStore.edit { prefs ->
            prefs[GuestUserIdKey] = newId
        }
        return newId
    }

                                                                    
    fun shortId(guestId: String): String =
        guestId.removePrefix("guest_").take(8).uppercase()

    fun isGuest(guestId: String): Boolean =
        guestId.startsWith("guest_")
}