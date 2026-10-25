/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.betsy.music.R



sealed class Screens(
    val route: String,
    @StringRes val titleId: Int,
    @DrawableRes val iconIdActive: Int,
    @DrawableRes val iconIdInactive: Int,
) {
    object Home : Screens(
        route = "home",
        titleId = R.string.home,
        iconIdActive = R.drawable.home,
        iconIdInactive = R.drawable.home,
    )
  
                             
                          
                                   
                                          
                                            
       

    
    object Search : Screens(
        route = "search_tab",
        titleId = R.string.search,
        iconIdActive = R.drawable.search,
        iconIdInactive = R.drawable.search,
    )

    object Library : Screens(
        route = "library",
        titleId = R.string.filter_library,
        iconIdActive = R.drawable.library_music,
        iconIdInactive = R.drawable.library_music,
    )

    object Settings : Screens(
        route = "settings",
        titleId = R.string.settings,
        iconIdActive = R.drawable.settings,
        iconIdInactive = R.drawable.settings,
    )

    companion object {
        
        val MainScreens = listOf(Home,Search, Library, Settings)
    }
}

