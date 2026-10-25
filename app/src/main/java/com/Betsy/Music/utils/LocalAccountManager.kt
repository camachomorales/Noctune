/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.utils

import android.os.Build
import android.util.Base64
import android.util.Patterns
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

   
                                                       
                                                                      
                                                                      
  
                                                                          
                                                                              
                                                                     
   
object LocalAccountManager {

    enum class AuthError { INVALID_EMAIL, PASSWORD_TOO_SHORT, EMAIL_TAKEN, WRONG_CREDENTIALS, NAME_EMPTY }

    sealed class AuthResult {
        data class Success(val accountId: String) : AuthResult()
        data class Failure(val error: AuthError) : AuthResult()
    }

    const val MIN_PASSWORD_LENGTH = 6
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    
    private fun idByEmailKey(email: String) = stringPreferencesKey("local_email:$email")
    private fun saltKey(id: String) = stringPreferencesKey("local_acc:$id:salt")
    private fun hashKey(id: String) = stringPreferencesKey("local_acc:$id:hash")
    private fun algKey(id: String) = stringPreferencesKey("local_acc:$id:alg")
    private fun nameKey(id: String) = stringPreferencesKey("local_acc:$id:name")
    private fun emailKey(id: String) = stringPreferencesKey("local_acc:$id:email")
    fun createdAtKey(id: String) = longPreferencesKey("local_acc:$id:createdAt")

    private fun normalize(email: String) = email.trim().lowercase()

    private fun defaultAlgorithm() =
        if (Build.VERSION.SDK_INT >= 26) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"

    private fun hash(password: String, salt: ByteArray, algorithm: String): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    suspend fun register(
        dataStore: DataStore<Preferences>,
        name: String,
        email: String,
        password: String,
    ): AuthResult {
        val cleanEmail = normalize(email)
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return AuthResult.Failure(AuthError.NAME_EMPTY)
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches())
            return AuthResult.Failure(AuthError.INVALID_EMAIL)
        if (password.length < MIN_PASSWORD_LENGTH)
            return AuthResult.Failure(AuthError.PASSWORD_TOO_SHORT)
        if (dataStore.data.first()[idByEmailKey(cleanEmail)] != null)
            return AuthResult.Failure(AuthError.EMAIL_TAKEN)

        val id = "local_${UUID.randomUUID()}"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val alg = defaultAlgorithm()
        val hashed = withContext(Dispatchers.Default) { hash(password, salt, alg) }

        dataStore.edit { p ->
            p[idByEmailKey(cleanEmail)] = id
            p[saltKey(id)] = Base64.encodeToString(salt, Base64.NO_WRAP)
            p[hashKey(id)] = Base64.encodeToString(hashed, Base64.NO_WRAP)
            p[algKey(id)] = alg
            p[nameKey(id)] = cleanName
            p[emailKey(id)] = cleanEmail
            p[createdAtKey(id)] = System.currentTimeMillis()
        }
        activate(dataStore, id, cleanName, cleanEmail)
        return AuthResult.Success(id)
    }

    suspend fun login(
        dataStore: DataStore<Preferences>,
        email: String,
        password: String,
    ): AuthResult {
        val cleanEmail = normalize(email)
        val prefs = dataStore.data.first()
        val id = prefs[idByEmailKey(cleanEmail)]
            ?: return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)
        val saltB64 = prefs[saltKey(id)]
        val hashB64 = prefs[hashKey(id)]
        if (saltB64 == null || hashB64 == null) return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)

        val alg = prefs[algKey(id)] ?: "PBKDF2WithHmacSHA1"
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val actual = withContext(Dispatchers.Default) {
            hash(password, Base64.decode(saltB64, Base64.NO_WRAP), alg)
        }
        
        if (!MessageDigest.isEqual(expected, actual))
            return AuthResult.Failure(AuthError.WRONG_CREDENTIALS)

        activate(dataStore, id, prefs[nameKey(id)].orEmpty(), prefs[emailKey(id)] ?: cleanEmail)
        return AuthResult.Success(id)
    }

                                                                                                   
    private suspend fun activate(dataStore: DataStore<Preferences>, id: String, name: String, email: String) {
        AccountManager.saveAccount(
            dataStore = dataStore,
            accountId = id,
            name = name,
            email = email,
            handle = "",
        )
    }

    suspend fun logout(dataStore: DataStore<Preferences>) {
        val guestId = GuestManager.getOrCreateGuestId(dataStore)
        val activeId = AccountManager.getActiveAccountId(dataStore)
        if (!GuestManager.isGuest(activeId)) {
            AccountManager.logoutAccount(dataStore, accountId = activeId, guestId = guestId)
        }
    }
}

                                                                                                      
@Composable
fun rememberIsLoggedIn(): Boolean {
    val (activeId) = rememberPreference(stringPreferencesKey("active_account_id"), "")
    return activeId.isNotBlank() && !GuestManager.isGuest(activeId)
}