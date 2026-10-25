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
import com.betsy.audius.compat.models.YTItem
import com.betsy.audius.compat.models.filterExplicit
import com.betsy.music.constants.HideExplicitKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.db.entities.SearchHistory
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: musicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    val ownerId = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        database.searchHistory(ownerId.value).map { history ->
                            SearchSuggestionViewState(
                                history = history,
                            )
                        }
                    } else {
                        val result = AudiusApi.searchSuggestions(query).getOrNull()
                        database
                            .searchHistory(query, ownerId.value)
                            .map { it.take(3) }
                            .map { history ->
                                SearchSuggestionViewState(
                                    history = history,
                                    suggestions =
                                        result
                                            ?.queries
                                            ?.filter { query ->
                                                history.none { it.query == query }
                                            }.orEmpty(),
                                    items =
                                        result
                                            ?.recommendedItems
                                            ?.filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false,
                                                ),
                                            ).orEmpty(),
                                )
                            }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)