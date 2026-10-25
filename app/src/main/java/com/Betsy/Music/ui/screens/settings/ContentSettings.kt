/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.betsy.music.NotificationPermissionPreference
import com.betsy.music.R
import com.betsy.music.constants.ContentLanguageKey
import com.betsy.music.constants.EnableLrcLibKey
import com.betsy.music.constants.HideExplicitKey
import com.betsy.music.constants.HistoryDuration
import com.betsy.music.constants.LanguageCodeToName
import com.betsy.music.constants.PreferredLyricsProvider
import com.betsy.music.constants.PreferredLyricsProviderKey
import com.betsy.music.constants.ProxyEnabledKey
import com.betsy.music.constants.ProxyTypeKey
import com.betsy.music.constants.ProxyUrlKey
import com.betsy.music.constants.QuickPicks
import com.betsy.music.constants.QuickPicksKey
import com.betsy.music.constants.SYSTEM_DEFAULT
import com.betsy.music.constants.TopSize
import com.betsy.music.ui.component.EditTextPreference
import com.betsy.music.ui.component.ListPreference
import com.betsy.music.ui.component.SettingsGeneralCategory
import com.betsy.music.ui.component.SettingsPage
import com.betsy.music.ui.component.SliderPreference
import com.betsy.music.ui.component.SwitchPreference
import com.betsy.music.utils.rememberEnumPreference
import com.betsy.music.utils.rememberPreference
import java.net.Proxy


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (contentLanguage, onContentLanguageChange) = rememberPreference(
        key = ContentLanguageKey,
        defaultValue = SYSTEM_DEFAULT
    )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(
        key = HideExplicitKey,
        defaultValue = false
    )
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(
        key = ProxyEnabledKey,
        defaultValue = false
    )
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(
        key = ProxyTypeKey,
        defaultValue = Proxy.Type.HTTP
    )
    val (proxyUrl, onProxyUrlChange) = rememberPreference(
        key = ProxyUrlKey,
        defaultValue = "host:port"
    )
    val (lengthTop, onLengthTopChange) = rememberPreference(
        key = TopSize,
        defaultValue = "50"
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        key = HistoryDuration,
        defaultValue = 30f
    )
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(
        key = QuickPicksKey,
        defaultValue = QuickPicks.QUICK_PICKS
    )
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(
        key = EnableLrcLibKey,
        defaultValue = true
    )
    val (preferredProvider, onPreferredProviderChange) = rememberEnumPreference(
        key = PreferredLyricsProviderKey,
        defaultValue = PreferredLyricsProvider.LRCLIB
    )


    SettingsPage(
        title = stringResource(R.string.content),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        
        SettingsGeneralCategory(
            title = stringResource(R.string.general),
            items = listOf(
                {ListPreference(
                    title = { Text(stringResource(R.string.content_language)) },
                    icon = { Icon(painterResource(R.drawable.language), null) },
                    selectedValue = contentLanguage,
                    values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                    valueText = {
                        LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                    },
                    onValueSelected = onContentLanguageChange,
                )},

                
                {SwitchPreference(
                    title = { Text(stringResource(R.string.hide_explicit)) },
                    icon = { Icon(painterResource(R.drawable.explicit), null) },
                    checked = hideExplicit,
                    onCheckedChange = onHideExplicitChange,
                )},

                {NotificationPermissionPreference()},
            )
        )

        
        SettingsGeneralCategory(
            title = stringResource(R.string.proxy),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_proxy)) },
                    icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                    checked = proxyEnabled,
                    onCheckedChange = onProxyEnabledChange,
                )},
                {if (proxyEnabled) {
                    Column {
                        ListPreference(
                            title = { Text(stringResource(R.string.proxy_type)) },
                            selectedValue = proxyType,
                            values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                            valueText = { it.name },
                            onValueSelected = onProxyTypeChange,
                        )
                        EditTextPreference(
                            title = { Text(stringResource(R.string.proxy_url)) },
                            value = proxyUrl,
                            onValueChange = onProxyUrlChange,
                        )
                    }
                }}
            )
        )

        
        SettingsGeneralCategory(
            title = stringResource(R.string.lyrics),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_lrclib)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enableLrclib,
                    onCheckedChange = onEnableLrclibChange,
                )},
            )
        )

        
        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {EditTextPreference(
                    title = { Text(stringResource(R.string.top_length)) },
                    icon = { Icon(painterResource(R.drawable.trending_up), null) },
                    value = lengthTop,
                    isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                    onValueChange = onLengthTopChange,
                )},
                {ListPreference(
                    title = { Text(stringResource(R.string.set_quick_picks)) },
                    icon = { Icon(painterResource(R.drawable.home_outlined), null) },
                    selectedValue = quickPicks,
                    values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
                    valueText = {
                        when (it) {
                            QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                            QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                        }
                    },
                    onValueSelected = onQuickPicksChange,
                )},
                {SliderPreference(
                    title = { Text(stringResource(R.string.history_duration)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    value = historyDuration,
                    onValueChange = onHistoryDurationChange,
                )},
            )
        )
    }
}