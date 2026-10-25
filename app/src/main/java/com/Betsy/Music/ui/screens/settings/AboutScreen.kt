/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.betsy.music.BuildConfig
import com.betsy.music.utils.rememberPreference
import com.betsy.music.R



@Composable
internal fun aboutAccentColor(): Color {
    val (accentColorInt) = rememberPreference(
        com.betsy.music.constants.AccentColorKey,
        defaultValue = 0xFF3b9cac.toInt()
    )
    return remember(accentColorInt) {
        Color(accentColorInt.toLong() or 0xFF000000L)
    }
}







                                                                  
private const val APP_NAME = "Noctune"

                                                                                        
private const val LICENSE_URL  = "https://www.gnu.org/licenses/gpl-3.0.html"

   
                                       
                                  
   
private object DevLinks {
    val FACEBOOK  : String? = null
    val INSTAGRAM : String? = null
    val GITHUB    : String? = com.betsy.music.utils.AppLinks.REPO_URL
    val PAYPAL    : String? = null
    val GOOGLE    : String? = null
    val WEBSITE   : String? = null
}

   
                                     
                                                                                           
   
private val COLLABORATORS = listOf(
    Collaborator(
        name       = "Betsy",
        role       = R.string.about_role_development,
        avatarUrl  = null,
        profileUrl = "https://github.com/camachomorales"
    ),
    Collaborator(
        name       = "ViMusic (vfsfitvnm)",
        role       = R.string.about_role_bottomsheet,
        avatarUrl  = null,
        profileUrl = "https://github.com/vfsfitvnm/Vimusic"
    ),
    Collaborator(
        name       = "OpenTune (Arturo254)",
        role       = R.string.about_role_ui_base,
        avatarUrl  = null,
        profileUrl = "https://github.com/Arturo254/OpenTune"
    ),
    Collaborator(
        name       = "InnerTune",
        role       = R.string.about_role_original_project,
        avatarUrl  = null,
        profileUrl = "https://github.com/z-huang/InnerTune"
    ),
)





data class Collaborator(
    val name       : String,
    @StringRes val role :  Int,
    val avatarUrl  : String?,
    val profileUrl : String
)





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController  : NavController,
    scrollBehavior : TopAppBarScrollBehavior? = null
) {
    val uriHandler = LocalUriHandler.current

    
    val accent = aboutAccentColor()
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back),
                        tint               = adaptiveColor
                    )
                }
                Text(
                    text       = stringResource(R.string.about),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = adaptiveColor,
                    modifier   = Modifier.align(Alignment.Center)
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding      = PaddingValues(bottom = 48.dp, top = 8.dp)
        ) {

            
            item {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(adaptiveColor.copy(alpha = 0.10f))
                        .border(2.dp, accent.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = APP_NAME,
                        tint               = adaptiveColor,
                        modifier           = Modifier.size(104.dp)
                    )
                }
            }

            
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = APP_NAME,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color      = adaptiveColor
                    )

                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        VersionBadge(label = BuildConfig.VERSION_NAME, accent = accent)
                        if (BuildConfig.DEBUG) {
                            VersionBadge(label = "DEBUG", accent = Color(0xFFFF6B6B))
                        }
                    }

                    Text(
                        text      = stringResource(R.string.about_app_description),
                        fontSize  = 13.sp,
                        color     = adaptiveColor.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            
            item {
                SocialLinksBar(accent = accent, uriHandler = { url -> uriHandler.openUri(url) })
            }

            
            item {
                HorizontalDivider(
                    color     = adaptiveColor.copy(alpha = 0.10f),
                    thickness = 1.dp,
                    modifier  = Modifier.padding(vertical = 4.dp)
                )
            }

            
            item {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.Center,
                    modifier               = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.person),
                        contentDescription = null,
                        tint               = adaptiveColor.copy(alpha = 0.7f),
                        modifier           = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = stringResource(R.string.contributors),
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = adaptiveColor
                    )
                }
            }

            
            items(COLLABORATORS) { collaborator ->
                CollaboratorCard(
                    collaborator = collaborator,
                    accent       = accent,
                    onClick      = { uriHandler.openUri(collaborator.profileUrl) }
                )
            }

            
            item {
                LegalCard(
                    accent    = accent,
                    onOpenUrl = { url -> uriHandler.openUri(url) }
                )
            }
        }
    }
}





                                                                                      
@Composable
private fun LegalCard(accent: Color, onOpenUrl: (String) -> Unit) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(adaptiveColor.copy(alpha = 0.06f))
            .border(1.dp, adaptiveColor.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text       = stringResource(R.string.about_license_title),
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = adaptiveColor
        )
        Text(
            text     = stringResource(
                R.string.about_license_text,
                APP_NAME,
                stringResource(R.string.about_license_name)
            ),
            fontSize = 13.sp,
            color    = adaptiveColor.copy(alpha = 0.75f)
        )
        Text(
            text       = stringResource(R.string.about_view_license),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = accent,
            modifier   = Modifier.clickable { onOpenUrl(LICENSE_URL) }
        )
        Text(
            text       = stringResource(R.string.about_source_code),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = accent,
            modifier   = Modifier.clickable { onOpenUrl(com.betsy.music.utils.AppLinks.REPO_URL) }
        )
        HorizontalDivider(color = adaptiveColor.copy(alpha = 0.10f), thickness = 1.dp)
        Text(
            text     = stringResource(R.string.about_third_party_services),
            fontSize = 12.sp,
            color    = adaptiveColor.copy(alpha = 0.55f)
        )
    }
}

                                    
@Composable
private fun VersionBadge(label: String, accent: Color) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            color      = adaptiveColor.copy(alpha = 0.75f)
        )
    }
}

                                                    
@Composable
private fun SocialLinksBar(accent: Color, uriHandler: (String) -> Unit) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()

    
    data class SocialItem(val iconRes: Int, val url: String, val label: String)

    val items = listOfNotNull(
        DevLinks.FACEBOOK ?.let { SocialItem(R.drawable.facebook,  it, "Facebook") },
        DevLinks.INSTAGRAM?.let { SocialItem(R.drawable.instagram, it, "Instagram") },
        DevLinks.GITHUB   ?.let { SocialItem(R.drawable.github,    it, "GitHub") },
        DevLinks.PAYPAL   ?.let { SocialItem(R.drawable.paypal,    it, "PayPal") },
        DevLinks.GOOGLE   ?.let { SocialItem(R.drawable.google,    it, "Google") },
        DevLinks.WEBSITE  ?.let { SocialItem(R.drawable.language,  it, stringResource(R.string.website)) },
    )

    if (items.isEmpty()) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(adaptiveColor.copy(alpha = 0.06f))
                .border(1.dp, adaptiveColor.copy(alpha = 0.10f), RoundedCornerShape(50.dp))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text     = "Agrega tus redes en DevLinks ↑",  
                fontSize = 12.sp,
                color    = adaptiveColor.copy(alpha = 0.35f)
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(adaptiveColor.copy(alpha = 0.08f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { uriHandler(item.url) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter            = painterResource(item.iconRes),
                    contentDescription = item.label,
                    tint               = adaptiveColor.copy(alpha = 0.80f),
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
    }
}

                                
@Composable
private fun CollaboratorCard(
    collaborator : Collaborator,
    accent       : Color,
    onClick      : () -> Unit
) {
    val adaptiveColor = com.betsy.music.ui.theme.rememberAdaptiveOnColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(adaptiveColor.copy(alpha = 0.07f))
            .border(1.dp, adaptiveColor.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(adaptiveColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (collaborator.avatarUrl != null) {
                AsyncImage(
                    model              = collaborator.avatarUrl,
                    contentDescription = collaborator.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                
                val initials = remember(collaborator.name) {
                    collaborator.name
                        .split(" ", "@")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "?" }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accent, Color(0xFF0A6EBD)),
                                start  = Offset(0f, 0f),
                                end    = Offset(80f, 80f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = initials,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = adaptiveColor
                    )
                }
            }
        }

        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter            = painterResource(R.drawable.person),
                    contentDescription = null,
                    tint               = accent,
                    modifier           = Modifier.size(14.dp)
                )
                Text(
                    text       = collaborator.name,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = adaptiveColor
                )
            }
            Text(
                text     = stringResource(collaborator.role),
                fontSize = 13.sp,
                color    = adaptiveColor.copy(alpha = 0.50f)
            )
        }

        
        Icon(
            painter            = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint               = adaptiveColor.copy(alpha = 0.30f),
            modifier           = Modifier.size(18.dp)
        )
    }
}