/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.music.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.betsy.music.R
import java.util.Locale

   
                                                                                                
                                                                                            
                                                                                   
                                                     
   
fun localizeStreamingTitleWith(ctx: Context, apiTitle: String): String =
    when (apiTitle.trim().lowercase()) {
        "top result", "top results",
        "resultado principal", "mejor resultado"          -> ctx.getString(R.string.search_top_result)
        "songs", "canciones"                              -> ctx.getString(R.string.filter_songs)
        "videos", "vídeos"                                -> ctx.getString(R.string.filter_videos)
        "albums", "álbumes", "albumes"                    -> ctx.getString(R.string.filter_albums)
        "artists", "artistas"                             -> ctx.getString(R.string.filter_artists)
        "playlists", "listas", "listas de reproducción"   -> ctx.getString(R.string.filter_playlists)
        "community playlists",
        "listas de la comunidad",
        "listas de reproducción de la comunidad"          -> ctx.getString(R.string.filter_community_playlists)
        "featured playlists",
        "listas destacadas",
        "listas de reproducción destacadas"               -> ctx.getString(R.string.filter_featured_playlists)
        "podcasts"                                        -> ctx.getString(R.string.search_podcasts)
        "episodes", "episodios"                           -> ctx.getString(R.string.search_episodes)
        "profiles", "perfiles"                            -> ctx.getString(R.string.search_profiles)
        "other", "otros"                                  -> ctx.getString(R.string.search_other)
        "tendencias de la semana"                         -> ctx.getString(R.string.section_trending_week)
        "álbumes del momento", "albumes del momento"      -> ctx.getString(R.string.section_albums_now)
        "listas para descubrir"                           -> ctx.getString(R.string.section_playlists_discover)
        else -> {
            
            val genre = apiTitle.trim()
                .takeIf { it.startsWith("En ") && it.length > 3 }
                ?.removePrefix("En ")
                ?.trim()
            if (genre != null) ctx.getString(R.string.section_in_genre, genre) else apiTitle
        }
    }

   
                                                                                          
                                                                                   
   
private fun contentLanguageContext(ctx: Context, language: String?): Context {
    if (language.isNullOrBlank()) return ctx
    return try {
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        ctx.createConfigurationContext(config)
    } catch (e: Exception) {
        ctx
    }
}

@Composable
fun localizedStreamingTitle(apiTitle: String): String {
    val ctx = LocalContext.current
    val contentLanguage = ContentLocale.explicitLanguage
    return remember(apiTitle, contentLanguage) {
        localizeStreamingTitleWith(contentLanguageContext(ctx, contentLanguage), apiTitle)
    }
}
