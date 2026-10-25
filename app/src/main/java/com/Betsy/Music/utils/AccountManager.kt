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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.betsy.music.constants.GuestUserIdKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID

   
                                             
  
                           
                                                                                
                                                      
                                                           
                                                      
                                                     
                                                             
                                                                     
  
                                                                                      
                                                                                                   
   
object AccountManager {

    

    private val activeAccountIdKey = stringPreferencesKey("active_account_id")

    private fun nameKey(accountId: String) =
        stringPreferencesKey("account:${accountId}:name")

    private fun emailKey(accountId: String) =
        stringPreferencesKey("account:${accountId}:email")

    private fun handleKey(accountId: String) =
        stringPreferencesKey("account:${accountId}:handle")

    private fun dataSyncIdKey(accountId: String) =
        stringPreferencesKey("account:${accountId}:dataSyncId")

    
    
    

    private val legacyCookieKey      = stringPreferencesKey("innerTubeCookie")
    private val legacyDataSyncIdKey  = stringPreferencesKey("dataSyncId")
    private val legacyVisitorDataKey = stringPreferencesKey("visitorData")
    private val legacyNameKey        = stringPreferencesKey("accountName")
    private val legacyEmailKey       = stringPreferencesKey("accountEmail")
    private val legacyHandleKey      = stringPreferencesKey("accountChannelHandle")

    

    suspend fun getActiveAccountId(dataStore: DataStore<Preferences>): String {
        return dataStore.data.first()[activeAccountIdKey]
            ?: GuestManager.getOrCreateGuestId(dataStore)
    }

    suspend fun setActiveAccount(dataStore: DataStore<Preferences>, accountId: String) {
        dataStore.edit { it[activeAccountIdKey] = accountId }
    }

    

    suspend fun saveAccount(
        dataStore: DataStore<Preferences>,
        accountId: String,
        name: String,
        email: String,
        handle: String,
    ) {
        dataStore.edit { prefs ->
            
            prefs[nameKey(accountId)]        = name
            prefs[emailKey(accountId)]       = email
            prefs[handleKey(accountId)]      = handle
            prefs[dataSyncIdKey(accountId)]  = accountId
            prefs[activeAccountIdKey]        = accountId

            
            
            
            prefs[legacyDataSyncIdKey]  = accountId
            prefs[legacyNameKey]        = name
            prefs[legacyEmailKey]       = email
            prefs[legacyHandleKey]      = handle
        }
    }

    

    suspend fun getActiveDataSyncId(dataStore: DataStore<Preferences>): String {
        val id = getActiveAccountId(dataStore)
        if (GuestManager.isGuest(id)) return ""
        return dataStore.data.first()[dataSyncIdKey(id)] ?: ""
    }

    data class AccountInfo(
        val id: String,
        val name: String,
        val email: String,
        val handle: String,
        val isGuest: Boolean,
    )

    suspend fun getActiveAccountInfo(dataStore: DataStore<Preferences>): AccountInfo {
        val id = getActiveAccountId(dataStore)
        val prefs = dataStore.data.first()
        return AccountInfo(
            id      = id,
            name    = prefs[nameKey(id)] ?: "",
            email   = prefs[emailKey(id)] ?: "",
            handle  = prefs[handleKey(id)] ?: "",
            isGuest = GuestManager.isGuest(id),
        )
    }

                                                            
    suspend fun getSavedAccounts(dataStore: DataStore<Preferences>): List<AccountInfo> {
        val prefs = dataStore.data.first()
        return prefs.asMap().keys
            .filter { it.name.startsWith("account:") && it.name.endsWith(":name") }
            .mapNotNull { key ->
                val id = key.name.removePrefix("account:").removeSuffix(":name")
                if (GuestManager.isGuest(id)) return@mapNotNull null
                AccountInfo(
                    id      = id,
                    name    = prefs[stringPreferencesKey("account:${id}:name")] ?: "",
                    email   = prefs[stringPreferencesKey("account:${id}:email")] ?: "",
                    handle  = prefs[stringPreferencesKey("account:${id}:handle")] ?: "",
                    isGuest = false,
                )
            }
    }

    

       
                                                                           
                                                                          
                                                                            
                                                        
      
                                                                        
                                                                      
       
    suspend fun logoutAccount(
        dataStore: DataStore<Preferences>,
        accountId: String,
        guestId: String,
    ) {
        dataStore.edit { prefs ->
            
            prefs.remove(dataSyncIdKey(accountId))
            prefs[activeAccountIdKey] = guestId

            
            prefs.remove(legacyCookieKey)
            prefs.remove(legacyDataSyncIdKey)
            prefs.remove(legacyVisitorDataKey)
            prefs.remove(legacyNameKey)
            prefs.remove(legacyEmailKey)
            prefs.remove(legacyHandleKey)
        }
    }

       
                                                                        
                                                                        
       
    suspend fun deleteAccount(
        dataStore: DataStore<Preferences>,
        accountId: String,
        guestId: String,
    ) {
        dataStore.edit { prefs ->
            prefs.remove(dataSyncIdKey(accountId))
            prefs.remove(nameKey(accountId))
            prefs.remove(emailKey(accountId))
            prefs.remove(handleKey(accountId))
            if (prefs[activeAccountIdKey] == accountId) {
                prefs[activeAccountIdKey] = guestId
                
                prefs.remove(legacyCookieKey)
                prefs.remove(legacyDataSyncIdKey)
                prefs.remove(legacyVisitorDataKey)
                prefs.remove(legacyNameKey)
                prefs.remove(legacyEmailKey)
                prefs.remove(legacyHandleKey)
            }
        }
    }

    

       
                                                                           
                                                                    
                                              
       
    suspend fun migrateFromLegacyIfNeeded(dataStore: DataStore<Preferences>) {
        // Borra cualquier cookie / visitorData que hayan dejado versiones anteriores
        // (InnerTune / OpenTune). Noctune ya no usa credenciales de YouTube.
        purgeLegacyCredentials(dataStore)

        val prefs = dataStore.data.first()
        if (prefs[activeAccountIdKey] != null) return

        val guestId = prefs[GuestUserIdKey]
            ?: "guest_${UUID.randomUUID()}".also { newId ->
                dataStore.edit { it[GuestUserIdKey] = newId }
            }

        dataStore.edit { p ->
            // Instalación nueva o actualización desde una versión con cuenta de YouTube:
            // se descartan los datos de esa cuenta y se arranca en modo invitado.
            p.remove(legacyDataSyncIdKey)
            p.remove(legacyNameKey)
            p.remove(legacyEmailKey)
            p.remove(legacyHandleKey)
            p[activeAccountIdKey] = guestId
        }
    }

    private suspend fun purgeLegacyCredentials(dataStore: DataStore<Preferences>) {
        dataStore.edit { prefs ->
            prefs.remove(legacyCookieKey)
            prefs.remove(legacyVisitorDataKey)
            prefs.asMap().keys
                .filter {
                    it.name.startsWith("account:") &&
                        (it.name.endsWith(":cookie") || it.name.endsWith(":visitorData"))
                }
                .forEach { prefs.remove(it) }
        }
    }
}
