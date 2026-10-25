/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults.contentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.betsy.music.R
import com.betsy.music.constants.AccentColorKey
import com.betsy.music.constants.AnimateLyricsKey
import com.betsy.music.constants.ChipSortTypeKey
import com.betsy.music.constants.DarkModeKey
import com.betsy.music.constants.DefaultMiniPlayerThumbnailShape
import com.betsy.music.constants.DefaultOpenTabKey
import com.betsy.music.constants.DefaultPlayPauseButtonShape
import com.betsy.music.constants.DefaultSmallButtonsShape
import com.betsy.music.constants.DynamicThemeKey
import com.betsy.music.constants.GridItemSize
import com.betsy.music.constants.GridItemsSizeKey
import com.betsy.music.constants.LibraryFilter
import com.betsy.music.constants.LyricsClickKey
import com.betsy.music.constants.LyricsTextPositionKey
import com.betsy.music.constants.MiniPlayerThumbnailShapeKey
import com.betsy.music.constants.PlayPauseButtonShapeKey
import com.betsy.music.constants.PlayerBackgroundStyle
import com.betsy.music.constants.PlayerBackgroundStyleKey
import com.betsy.music.constants.PlayerButtonsStyle
import com.betsy.music.constants.PlayerButtonsStyleKey
import com.betsy.music.constants.PlayerTextAlignmentKey
import com.betsy.music.constants.PureBlackKey
import com.betsy.music.constants.RotateBackgroundKey
import com.betsy.music.constants.SliderStyle
import com.betsy.music.constants.SliderStyleKey
import com.betsy.music.constants.SlimNavBarKey
import com.betsy.music.constants.SmallButtonsShapeKey
import com.betsy.music.constants.SwipeThumbnailKey
import com.betsy.music.ui.component.AvatarSelector
import com.betsy.music.ui.component.DefaultDialog
import com.betsy.music.ui.component.EnumListPreference
import com.betsy.music.ui.component.LanguagePreference
import com.betsy.music.ui.component.ListPreference
import com.betsy.music.ui.component.PlayerSliderTrack
import com.betsy.music.ui.component.PreferenceEntry
import com.betsy.music.ui.component.SettingsGeneralCategory
import com.betsy.music.ui.component.SettingsPage
import com.betsy.music.ui.component.SwitchPreference
import com.betsy.music.ui.component.ThumbnailCornerRadiusSelectorButton
import com.betsy.music.ui.component.UnifiedShapeSelectorButton
import com.betsy.music.ui.theme.rememberAdaptiveOnColor
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import timber.log.Timber

val AccentColorOptions = listOf(
    0xFFFFFFFF.toInt() to "Blanco",
    0xFF000000.toInt() to "Negro",
    0xFF3b9cac.toInt() to "#3b9cac",
    0xFF237c8b.toInt() to "#237c8b",
    0xFF1a637c.toInt() to "#1a637c",
    0xFF044d65.toInt() to "#044d65",
    0xFF14547c.toInt() to "#14547c",
    0xFF042c54.toInt() to "#042c54",
    0xFF073c64.toInt() to "#073c64",
)

private fun Color.luminance(): Float =
    (0.2126 * red + 0.7152 * green + 0.0722 * blue).toFloat()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {

    val (accentColor, onAccentColorChange) = rememberPreference(
        AccentColorKey,
        defaultValue = 0xFF3b9cac.toInt()
    )

    val activeAccentColor = remember(accentColor) {
        Color(accentColor.toLong() or 0xFF000000L)
    }

    val (playerTextAlignment, onPlayerTextAlignmentChange) =
        rememberEnumPreference(
            PlayerTextAlignmentKey,
            defaultValue = PlayerTextAlignment.CENTER,
        )

    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.SQUIGGLY
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.BIG
    )
    val (animateLyrics, onAnimateLyricsChange) = rememberPreference(
        AnimateLyricsKey,
        defaultValue = true
    )
    val (rotateBackground, onRotateBackgroundChange) = rememberPreference(
        key = RotateBackgroundKey,
        defaultValue = false
    )

    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )
    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )
    val miniPlayerThumbnailShapeState = rememberPreference(
        key = MiniPlayerThumbnailShapeKey,
        defaultValue = DefaultMiniPlayerThumbnailShape
    )

    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = false)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }
      
                                      
                                                                    
           

    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = true)

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable { mutableStateOf(false) }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(onClick = { showSliderOptionDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = { showSliderOptionDialog = false }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f).weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == SliderStyle.DEFAULT) activeAccentColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable { onSliderStyleChange(SliderStyle.DEFAULT); showSliderOptionDialog = false }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember { mutableFloatStateOf(0.5f) }
                    Slider(value = sliderValue, valueRange = 0f..1f, onValueChange = { sliderValue = it }, modifier = Modifier.weight(1f), colors = com.betsy.music.ui.theme.accentSliderColors())
                    Text(text = stringResource(R.string.default_), style = MaterialTheme.typography.labelLarge)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f).weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == SliderStyle.SQUIGGLY) activeAccentColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable { onSliderStyleChange(SliderStyle.SQUIGGLY); showSliderOptionDialog = false }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember { mutableFloatStateOf(0.5f) }
                    SquigglySlider(value = sliderValue, valueRange = 0f..1f, onValueChange = { sliderValue = it }, modifier = Modifier.weight(1f), colors = com.betsy.music.ui.theme.accentSliderColors())
                    Text(text = stringResource(R.string.squiggly), style = MaterialTheme.typography.labelLarge)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f).weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (sliderStyle == SliderStyle.SLIM) activeAccentColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable { onSliderStyleChange(SliderStyle.SLIM); showSliderOptionDialog = false }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember { mutableFloatStateOf(0.5f) }
                    Slider(
                        value = sliderValue, valueRange = 0f..1f, onValueChange = { sliderValue = it },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = SliderDefaults.colors()) },
                        modifier = Modifier.weight(1f).pointerInput(Unit) { detectTapGestures(onPress = {}) }
                    )
                    Text(text = stringResource(R.string.slim), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    val cardColor = if (useDarkTheme) {
        Color.Black.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.22f)
    }

    val adaptiveOnColor = rememberAdaptiveOnColor()

    SettingsPage(
        title = stringResource(R.string.appearance),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.theme),
            items = listOf(
                {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.palette),
                                contentDescription = null,
                                tint = activeAccentColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 0.dp)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.accent_color_label),
                                style = MaterialTheme.typography.bodyLarge,
                                
                                color = adaptiveOnColor
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp)
                        ) {
                            AccentColorOptions.forEach { (colorInt, _) ->
                                val isSelected = accentColor == colorInt
                                val circleColor = Color(colorInt.toLong() or 0xFF000000L)
                                val checkColor = if (circleColor.luminance() > 0.4f) {
                                    adaptiveOnColor
                                } else {
                                    Color.White
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(circleColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) activeAccentColor
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { onAccentColorChange(colorInt) }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(R.drawable.check),
                                            contentDescription = null,
                                            tint = checkColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.dark_theme)) },
                        icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                        selectedValue = darkMode,
                        onValueSelected = onDarkModeChange,
                        valueText = {
                            when (it) {
                                DarkMode.ON -> stringResource(R.string.dark_theme_on)
                                DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                                DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                            }
                        },
                    )
                },
                {       
                                                      
                                         
                                                                                  
                                                                                        
                                                                
                                                           
                                                                             
                              
                                                    
                         
                       
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.app_language),
            items = listOf({ LanguagePreference() })
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.default_open_tab)) },
                        icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                        selectedValue = defaultOpenTab,
                        onValueSelected = onDefaultOpenTabChange,
                        valueText = {
                            when (it) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.SEARCH -> stringResource(R.string.search)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        },
                    )
                },
                {
                    ListPreference(
                        title = { Text(stringResource(R.string.default_lib_chips)) },
                        icon = { Icon(painterResource(R.drawable.tab), null) },
                        selectedValue = defaultChip,
                        values = listOf(
                            LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS,
                            LibraryFilter.ALBUMS, LibraryFilter.ARTISTS
                        ),
                        valueText = {
                            when (it) {
                                LibraryFilter.SONGS -> stringResource(R.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        },
                        onValueSelected = onDefaultChipChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.slim_navbar)) },
                        icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                        checked = slimNav,
                        onCheckedChange = onSlimNavChange
                    )
                },
                {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.grid_cell_size)) },
                        icon = { Icon(painterResource(R.drawable.grid_view), null) },
                        selectedValue = gridItemSize,
                        onValueSelected = onGridItemSizeChange,
                        valueText = {
                            when (it) {
                                GridItemSize.SMALL -> stringResource(R.string.small)
                                GridItemSize.BIG -> stringResource(R.string.big)
                            }
                        },
                    )
                },
            )
        )

        AvatarSelector(modifier = Modifier.padding(vertical = 8.dp))
    }
}

enum class DarkMode { ON, OFF, AUTO }
enum class NavigationTab { HOME, SEARCH, LIBRARY }
enum class LyricsPosition { LEFT, CENTER, RIGHT }
enum class PlayerTextAlignment { SIDED, CENTER }