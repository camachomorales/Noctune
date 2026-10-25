/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

package com.betsy.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.pages.MoodAndGenres
import com.betsy.music.utils.ContentLocale
import com.betsy.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    init {
        viewModelScope.launch {
            ContentLocale.flow(context).collectLatest {
                AudiusApi
                    .moodAndGenres()
                    .onSuccess {
                        moodAndGenres.value = it
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }
}