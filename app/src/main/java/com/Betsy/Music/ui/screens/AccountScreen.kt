/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.betsy.music.R
import com.betsy.music.ui.component.AccountContent
import com.betsy.music.ui.component.SettingsPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    SettingsPage(
        title = stringResource(R.string.account),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        AccountContent(navController = navController)
    }
}