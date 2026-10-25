/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.betsy.music.LocalPlayerAwareWindowInsets
import com.betsy.music.ui.utils.isScrollingUp

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyListState,
    @DrawableRes icon: Int,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible && lazyListState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
    ) {
        FloatingActionButton(
            modifier = Modifier.padding(16.dp),
            containerColor = containerColor ?: FloatingActionButtonDefaults.containerColor,
            contentColor = contentColor ?: androidx.compose.material3.contentColorFor(
                containerColor ?: FloatingActionButtonDefaults.containerColor
            ),
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyGridState,
    @DrawableRes icon: Int,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible && lazyListState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
    ) {
        FloatingActionButton(
            modifier = Modifier.padding(16.dp),
            containerColor = containerColor ?: FloatingActionButtonDefaults.containerColor,
            contentColor = contentColor ?: androidx.compose.material3.contentColorFor(
                containerColor ?: FloatingActionButtonDefaults.containerColor
            ),
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    scrollState: ScrollState,
    @DrawableRes icon: Int,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible && scrollState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
    ) {
        FloatingActionButton(
            modifier = Modifier.padding(16.dp),
            containerColor = containerColor ?: FloatingActionButtonDefaults.containerColor,
            contentColor = contentColor ?: androidx.compose.material3.contentColorFor(
                containerColor ?: FloatingActionButtonDefaults.containerColor
            ),
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
            )
        }
    }
}