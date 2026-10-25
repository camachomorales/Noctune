/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun SettingsCategory(
    title: String? = null,
    items: List<SettingsCategoryItem>
) {
    val (accentColorInt) = com.betsy.music.utils.rememberPreference(
        com.betsy.music.constants.AccentColorKey, defaultValue = 0xFF3b9cac.toInt()
    )
    val accentColor = remember(accentColorInt) {
        androidx.compose.ui.graphics.Color(accentColorInt.toLong() or 0xFF000000L)
    }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                modifier = Modifier.padding(start = 0.dp, bottom = 8.dp, top = 8.dp)
            )
        }

        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = adaptiveColor.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Material3SettingsItemRow(
                        item = item,
                        showDivider = index < items.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGeneralCategory(
    title: String? = null,
    items: List<@Composable () -> Unit>
) {
    val (accentColorInt) = com.betsy.music.utils.rememberPreference(
        com.betsy.music.constants.AccentColorKey, defaultValue = 0xFF3b9cac.toInt()
    )
    val accentColor = remember(accentColorInt) {
        androidx.compose.ui.graphics.Color(accentColorInt.toLong() or 0xFF000000L)
    }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                modifier = Modifier.padding(start = 0.dp, bottom = 8.dp, top = 8.dp)
            )
        }

        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = adaptiveColor.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    item()
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = 76.dp,
                                end = 20.dp
                            ),
                            thickness = 0.5.dp,
                            color = adaptiveColor.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

                                                           
@Composable
private fun Material3SettingsItemRow(
    item: SettingsCategoryItem,
    showDivider: Boolean
) {
    val (accentColorInt) = com.betsy.music.utils.rememberPreference(
        com.betsy.music.constants.AccentColorKey, defaultValue = 0xFF3b9cac.toInt()
    )
    val accentColor = remember(accentColorInt) {
        androidx.compose.ui.graphics.Color(accentColorInt.toLong() or 0xFF000000L)
    }
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    enabled = item.onClick != null,
                    onClick = { item.onClick?.invoke() }
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            item.icon?.let { icon ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            accentColor.copy(
                                alpha = if (item.isHighlighted) 0.15f else 0.1f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.showBadge) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            }
                        ) {
                            Icon(
                                painter = icon,
                                contentDescription = null,
                                tint = if (item.isHighlighted)
                                    accentColor
                                else
                                    accentColor.copy(alpha = 0.9f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = if (item.isHighlighted)
                                accentColor
                            else
                                accentColor.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                
                CompositionLocalProvider(LocalContentColor provides adaptiveColor) {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                        item.title()
                    }

                    
                    item.description?.let { desc ->
                        Spacer(modifier = Modifier.height(2.dp))
                        desc()
                    }
                }
            }

            
            item.trailingContent?.let { trailing ->
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }

        
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(
                    start = if (item.icon != null) 76.dp else 20.dp,
                    end = 20.dp
                ),
                thickness = 0.5.dp,
                color = adaptiveColor.copy(alpha = 0.15f)
            )
        }
    }
}

                                              
data class SettingsCategoryItem(
    val icon: Painter? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val onClick: (() -> Unit)? = null
)