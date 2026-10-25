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
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.StreamingLocale
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.constants.ContentCountryKey
import com.betsy.music.constants.ContentLanguageKey
import com.betsy.music.constants.CountryCodeToName
import com.betsy.music.constants.LanguageCodeToName
import com.betsy.music.constants.SYSTEM_DEFAULT
import com.betsy.music.App
import com.betsy.music.ui.component.LocaleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.Locale

   
                                                                         
                                                                                     
   
object ContentLocale {

       
                                                                                              
                                                                                                 
                        
       
    @Volatile
    var explicitLanguage: String? = null
        private set

       
                                                                                          
                                                                                            
                                                                              
       
    private fun appLanguageTag(): String? = try {
        val parts = LocaleManager.getInstance(App.instance)
            .getEffectiveLanguageCode()
            .replace("-r", "-")
            .split("-")
        val lang = parts[0].lowercase()
        val full = parts.getOrNull(1)?.let { "$lang-${it.uppercase()}" }
        listOfNotNull(full, lang).firstOrNull { it in LanguageCodeToName }
    } catch (e: Exception) {
        null
    }

    fun resolve(country: String?, language: String?): StreamingLocale {
        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "")
        return StreamingLocale(
            gl = country?.takeIf { it != SYSTEM_DEFAULT && it in CountryCodeToName }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = language?.takeIf { it != SYSTEM_DEFAULT && it in LanguageCodeToName }
                ?: appLanguageTag()
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
    }

    fun apply(country: String?, language: String?): StreamingLocale {
        val resolved = resolve(country, language)
        if (AudiusApi.locale != resolved) AudiusApi.locale = resolved

        
        val chosen = language?.takeIf { it != SYSTEM_DEFAULT && it in LanguageCodeToName }
        explicitLanguage = chosen
        
        InternetArchive.client.contentLanguage = chosen
        return resolved
    }

       
                                                                                          
                                                                                                  
       
    fun flow(context: Context): Flow<StreamingLocale> =
        context.dataStore.data
            .map { prefs -> prefs[ContentCountryKey] to prefs[ContentLanguageKey] }
            .distinctUntilChanged()
            .map { (country, language) -> apply(country, language) }
}