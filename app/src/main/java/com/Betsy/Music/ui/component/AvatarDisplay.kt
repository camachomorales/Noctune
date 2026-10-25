/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.betsy.music.R

   
                                                                         
      
   
@Composable
fun AvatarDisplay(
    size: Dp = 40.dp,
    showBorder: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    borderWidth: Dp = 2.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)

    val displayModifier = if (showBorder) {
        modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
    } else {
        modifier
            .size(size)
            .clip(CircleShape)
    }

    Box(
        modifier = displayModifier,
        contentAlignment = Alignment.Center
    ) {
        when (currentSelection) {
            is AvatarSelection.Custom -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                        .crossfade(true)
                        .error(R.drawable.person)
                        .placeholder(R.drawable.person)
                        .build(),
                    contentDescription = contentDescription
                        ?: stringResource(id = R.string.custom_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            is AvatarSelection.DiceBear -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data((currentSelection as AvatarSelection.DiceBear).url)
                        .crossfade(true)
                        .error(R.drawable.person)
                        .placeholder(R.drawable.person)
                        .build(),
                    contentDescription = contentDescription ?: stringResource(R.string.avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                DefaultAvatarIcon(contentDescription = contentDescription)
            }
        }
    }
}

                                                                 
@Composable
fun SmallAvatarDisplay(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    AvatarDisplay(
        size = 32.dp,
        showBorder = false,
        modifier = modifier,
        contentDescription = contentDescription
    )
}

                                                            
@Composable
fun LargeAvatarDisplay(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showBorder: Boolean = true
) {
    AvatarDisplay(
        size = 80.dp,
        showBorder = showBorder,
        borderWidth = 3.dp,
        modifier = modifier,
        contentDescription = contentDescription
    )
}

                                                    
@Composable
fun AvatarWithStatus(
    isOnline: Boolean = true,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Box(modifier = modifier) {
        AvatarDisplay(
            size = size,
            showBorder = true,
            contentDescription = contentDescription
        )

        
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = stringResource(R.string.online),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

                                  
@Composable
private fun DefaultAvatarIcon(contentDescription: String? = null) {
    Icon(
        painter = painterResource(id = R.drawable.person),
        contentDescription = contentDescription ?: stringResource(id = R.string.default_avatar),
        modifier = Modifier.fillMaxSize(0.6f),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

   
                                                                        
              
   
class AvatarUtils {
    companion object {
                                                                           
        fun getCustomAvatarUri(selection: AvatarSelection): String? {
            return when (selection) {
                is AvatarSelection.Custom -> selection.uri
                else -> null
            }
        }

                                                                      
        fun getDiceBearAvatarUrl(selection: AvatarSelection): String? {
            return when (selection) {
                is AvatarSelection.DiceBear -> selection.url
                else -> null
            }
        }

                                                            
        fun isCustomAvatar(selection: AvatarSelection): Boolean {
            return selection is AvatarSelection.Custom
        }

                                                       
        fun isDiceBearAvatar(selection: AvatarSelection): Boolean {
            return selection is AvatarSelection.DiceBear
        }

                                                          
        fun isDefaultAvatar(selection: AvatarSelection): Boolean {
            return selection is AvatarSelection.Default
        }

                                                     
        fun getAvatarSource(selection: AvatarSelection): String? {
            return when (selection) {
                is AvatarSelection.Custom -> selection.uri
                is AvatarSelection.DiceBear -> selection.url
                else -> null
            }
        }
    }
}