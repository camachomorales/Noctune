/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music

import android.app.Application
import android.content.Context
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.betsy.audius.compat.AudiusApi
import com.betsy.music.constants.ContentCountryKey
import com.betsy.music.constants.ContentLanguageKey
import com.betsy.music.constants.MaxImageCacheSizeKey
import com.betsy.music.constants.ProxyEnabledKey
import com.betsy.music.constants.ProxyTypeKey
import com.betsy.music.constants.ProxyUrlKey
import com.betsy.music.extensions.toEnum
import com.betsy.music.extensions.toInetSocketAddress
import com.betsy.music.utils.AccountManager
import com.betsy.music.utils.ContentLocale
import com.betsy.music.utils.GuestManager
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import com.betsy.music.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.Proxy
import java.util.Locale

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.plant(Timber.DebugTree())
        AudiusApi.init(appName = "BetsyMusic", apiKey = BuildConfig.AUDIUS_API_KEY)

        
        
        ContentLocale.apply(dataStore[ContentCountryKey], dataStore[ContentLanguageKey])
        
        
        GlobalScope.launch(Dispatchers.IO) {
            ContentLocale.flow(this@App).collect { }
        }

        
        if (dataStore[ProxyEnabledKey] == true) {
            try {
                AudiusApi.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        
        GlobalScope.launch(Dispatchers.IO) {
            
            AccountManager.migrateFromLegacyIfNeeded(dataStore)
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cacheSize = dataStore[MaxImageCacheSizeKey]
        if (cacheSize == 0) {
            return ImageLoader.Builder(this)
                .crossfade(true)
                .respectCacheHeaders(false)
                .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }
        return ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 512) * 1024 * 1024L)
                    .build()
            )
            .build()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}