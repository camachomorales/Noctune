/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.utils.rememberPreference

@Composable
fun NavigationTitle(
    title    : String,
    modifier : Modifier = Modifier,
    label    : String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick  : (() -> Unit)? = null,
) {
    val accentColorInt by rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    val accentColor = remember(accentColorInt) { Color(accentColorInt.toLong() or 0xFF000000L) }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        thumbnail?.invoke()

        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
            label?.let {
                Text(text = it, color = accentColor.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Medium, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                color      = accentColor,
                overflow   = TextOverflow.Ellipsis,
                maxLines   = 1,
            )
        }

        if (onClick != null) {
            Icon(
                painter            = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint               = accentColor,
            )
        }
    }
}

