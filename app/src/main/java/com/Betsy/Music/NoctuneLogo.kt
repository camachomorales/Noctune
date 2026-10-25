/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap

private val NoctuneBackground = Color(0xFF071F38)
private val NoctuneTurquoise = Color(0xFF48D5DF)
private val NoctuneDarkMountain = Color(0xFF082B50)
private val NoctuneMountain = Color(0xFF104B77)

private const val BAR_COUNT = 11


private val BAR_PROFILE = listOf(
    0.35f, 0.55f, 0.75f, 0.95f, 1.00f, 1.00f,
    0.90f, 0.80f, 0.60f, 0.45f, 0.30f
)



private val MOON_BARS = 3..7

@Composable
fun NoctuneLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ecualizador")

    
    
    val heights = List(BAR_COUNT) { i ->
        transition.animateFloat(
            initialValue = if (i in MOON_BARS) 0.80f else 0.30f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500 + i * 60,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "barra_$i"
        )
    }

    Canvas(
        modifier = modifier.background(NoctuneBackground)
    ) {
        val w = size.width
        val h = size.height

        
        val distantMountains = Path().apply {
            moveTo(0f, h * 0.76f)
            lineTo(w * 0.15f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.72f)
            lineTo(w * 0.43f, h * 0.60f)
            lineTo(w * 0.57f, h * 0.71f)
            lineTo(w * 0.76f, h * 0.63f)
            lineTo(w, h * 0.74f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(distantMountains, NoctuneMountain)

        
        val frontMountains = Path().apply {
            moveTo(0f, h * 0.83f)
            lineTo(w * 0.17f, h * 0.76f)
            lineTo(w * 0.32f, h * 0.85f)
            lineTo(w * 0.48f, h * 0.72f)
            lineTo(w * 0.65f, h * 0.84f)
            lineTo(w * 0.82f, h * 0.75f)
            lineTo(w, h * 0.82f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(frontMountains, NoctuneDarkMountain)

        
        val baseY = h * 0.47f
        val spacing = w / (BAR_COUNT + 1)
        val maxBarHeight = h * 0.30f
        val stroke = spacing * 0.58f 

        heights.forEachIndexed { i, state ->
            val x = spacing * (i + 1)
            
            val barHeight = (maxBarHeight * BAR_PROFILE[i] * state.value - stroke / 2f)
                .coerceAtLeast(0f)

            drawLine(
                color = NoctuneTurquoise,
                start = Offset(x, baseY - barHeight),
                end = Offset(x, baseY + barHeight),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        
        val moonCenter = Offset(w * 0.50f, baseY)
        val moonRadius = w * 0.20f

        val outerMoon = Path().apply {
            addOval(
                Rect(
                    moonCenter.x - moonRadius,
                    moonCenter.y - moonRadius,
                    moonCenter.x + moonRadius,
                    moonCenter.y + moonRadius
                )
            )
        }

        
        val cutRadius = moonRadius * 0.80f
        val cutCenter = Offset(
            x = moonCenter.x + moonRadius * 0.38f,
            y = moonCenter.y - moonRadius * 0.12f
        )

        val cutout = Path().apply {
            addOval(
                Rect(
                    cutCenter.x - cutRadius,
                    cutCenter.y - cutRadius,
                    cutCenter.x + cutRadius,
                    cutCenter.y + cutRadius
                )
            )
        }

        val crescent = Path.combine(PathOperation.Difference, outerMoon, cutout)

        
        drawPath(path = crescent, color = NoctuneBackground)
    }
}