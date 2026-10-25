/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.theme

import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.utils.rememberPreference

                                                                      
@Composable
fun rememberAccentColor(): Color {
    val (accentColorInt) = rememberPreference(AccentColorKey, defaultValue = 0xFF3b9cac.toInt())
    return remember(accentColorInt) {
        Color(accentColorInt.toLong() or 0xFF000000L)
    }
}

                                                                          
fun Color.onAccent(): Color = if (luminance() > 0.5f) Color.Black else Color.White

                                                                     
@Composable
fun accentSwitchColors(): SwitchColors {
    val accent = rememberAccentColor()
    val onAccent = accent.onAccent()
    return SwitchDefaults.colors(
        checkedThumbColor = onAccent,
        checkedTrackColor = accent,
        checkedBorderColor = accent,
        checkedIconColor = accent,
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f),
        uncheckedBorderColor = Color.Transparent,
        uncheckedIconColor = Color.Gray,
    )
}

                                          
@Composable
fun accentRadioColors(): RadioButtonColors {
    val accent = rememberAccentColor()
    return RadioButtonDefaults.colors(
        selectedColor = accent,
        unselectedColor = Color.Gray,
    )
}

                                     
@Composable
fun accentSliderColors(): SliderColors {
    val accent = rememberAccentColor()
    return SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        activeTickColor = accent.onAccent().copy(alpha = 0.5f),
        inactiveTrackColor = accent.copy(alpha = 0.25f),
        inactiveTickColor = accent.copy(alpha = 0.5f),
    )
}

                                                                               
@Composable
fun accentProgressTrackColor(): Color = rememberAccentColor().copy(alpha = 0.25f)
