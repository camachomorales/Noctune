/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.betsy.music.constants.GuestUserIdKey
import kotlinx.coroutines.launch
import java.util.UUID

   
                                                       
                                             
                                                                                  
                                                                           
   
@Composable
fun rememberGuestId(): String {
    val (storedGuestId, setGuestId) = rememberPreference(GuestUserIdKey, defaultValue = "")
    val coroutineScope = rememberCoroutineScope()

    val guestId = remember(storedGuestId) {
        if (storedGuestId.isNotBlank()) {
            storedGuestId
        } else {
            
            val newId = "guest_${UUID.randomUUID()}"
            coroutineScope.launch { setGuestId(newId) }
            newId
        }
    }
    return guestId
}