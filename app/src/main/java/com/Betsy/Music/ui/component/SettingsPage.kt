/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.R
import com.betsy.music.ui.utils.backToMain
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color

@ExperimentalMaterial3Api
@Composable
fun SettingsPage(
    title: String,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    items: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
        ) {
            items()
            Spacer(Modifier.height(16.dp))
        }

        SettingsTopAppBar(title, navController)
    }
}

@ExperimentalMaterial3Api
@Composable
fun SettingsTopAppBar(title: String, navController: NavController) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor(),
            navigationIconContentColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor(),
            actionIconContentColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor(),
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
    )
}