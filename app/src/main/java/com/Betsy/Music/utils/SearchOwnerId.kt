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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey

   
                                                             
                                                   
  
                                                                              
                                                                          
                                                                                    
   
@Composable
fun rememberSearchOwnerId(): String {
    val context = LocalContext.current
    val activeAccountIdKey = remember { stringPreferencesKey("active_account_id") }
    val prefs by context.dataStore.data.collectAsState(initial = emptyPreferences())
    val guestId = rememberGuestId()
    return remember(prefs, guestId) {
        val activeId = prefs[activeAccountIdKey] ?: ""
        activeId.ifBlank { guestId }
    }
}
