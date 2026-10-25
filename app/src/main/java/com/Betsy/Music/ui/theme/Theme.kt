/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.betsy.music.constants.PlayerBackgroundStyle
import com.google.material.color.dynamiccolor.DynamicScheme
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot
import com.google.material.color.score.Score

val DefaultThemeColor = Color(0xFF4285F4)


val AccentColorOptions = listOf(
    Color(0xFFFFFFFF), 
    Color(0xFF000000), 
    Color(0xFF3b9cac),
    Color(0xFF237c8b),
    Color(0xFF1a637c),
    Color(0xFF044d65),
    Color(0xFF14547c),
    Color(0xFF042c54),
    Color(0xFF073c64),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun musicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    expressive: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = remember(darkTheme, pureBlack, themeColor) {
        if (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) {
                dynamicDarkColorScheme(context).pureBlack(pureBlack, darkTheme)
            } else {
                dynamicLightColorScheme(context).pureBlack(false, darkTheme)
            }
        } else {
            SchemeTonalSpot(Hct.fromInt(themeColor.toArgb()), darkTheme, 0.0)
                .toColorScheme()
                .pureBlack(pureBlack, darkTheme)
        }
    }

    val motionScheme = if (expressive) {
        MotionScheme.expressive()
    } else {
        MotionScheme.standard()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        motionScheme = motionScheme,
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xFF4285F4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

object PlayerColorExtractor {
    fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int = Color(0xFF595959).toArgb()
    ): List<Color> {
        val extractedColors = palette.swatches
            .associate { it.rgb to it.population }

        val orderedColors = Score.score(extractedColors, 2, fallbackColor, true)
            .sortedByDescending { Color(it).luminance() }

        return if (orderedColors.size >= 2) {
            listOf(Color(orderedColors[0]), Color(orderedColors[1]))
        } else {
            listOf(Color(0xFF595959), Color(0xFF0D0D0D))
        }
    }
}

fun DynamicScheme.toColorScheme() =
    ColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
    )

fun ColorScheme.pureBlack(apply: Boolean, isDarkTheme: Boolean) =
    if (apply && isDarkTheme) {
        val translucentBlack = Color.Transparent.copy(alpha = 0.0f)
        copy(
            surface = translucentBlack,
            background = translucentBlack,
            surfaceContainer = translucentBlack,
            surfaceContainerLow = translucentBlack,
            surfaceContainerLowest = translucentBlack,
        )
    } else {
        this
    }

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

   
                                                                               
                                   
                                                      
                                              
  
                                                                              
                                                                
   
@Composable
fun rememberAdaptiveOnColor(): Color {
    val darkMode by com.betsy.music.utils.rememberEnumPreference(
        com.betsy.music.constants.DarkModeKey,
        defaultValue = com.betsy.music.ui.screens.settings.DarkMode.AUTO
    )
    
    
    return if (darkMode == com.betsy.music.ui.screens.settings.DarkMode.ON) {
        Color.Black
    } else {
        Color.White
    }
}

   
                                                                   
                                                                            
                                                                          
   
@Composable
fun rememberAdaptivePanelColor(alpha: Float = 0.08f): Color {
    return rememberAdaptiveOnColor().copy(alpha = alpha)
}

object PlayerSliderColors {
    @Composable
    fun getSliderColors(
        textButtonColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean
    ) = SliderDefaults.colors(
        activeTrackColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_music -> Color.White
        },
        inactiveTrackColor = when {
            useDarkTheme -> Color.Gray.copy(alpha = 0.5f)
            else -> Color.Gray.copy(alpha = 0.3f)
        },
        activeTickColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_music -> Color.White
        },
        inactiveTickColor = Color.Gray,
        thumbColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_music -> Color.White
        }
    )
}