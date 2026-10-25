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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.AlbumItem
import com.betsy.audius.compat.models.filterExplicit
import com.betsy.audius.compat.pages.SearchSummary
import com.betsy.audius.compat.pages.SearchSummaryPage
import com.betsy.internetarchive.InternetArchive
import com.betsy.music.constants.HideExplicitKey
import com.betsy.music.models.ItemsPage
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.get
import com.betsy.music.utils.reportException
import com.betsy.music.utils.toAlbumItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

   
                                                                               
                                                                                
                                                                                                    
   
@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<AudiusApi.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    
    private var audiusSummary: SearchSummaryPage? = null
    private var iaSummaryAlbums: List<AlbumItem> = emptyList()

    
    private var audiusAlbumsContinuation: String? = null
    private var iaNextPage: Int? = null
    private var loadingMore = false

    init {
        
        viewModelScope.launch {
            iaSummaryAlbums = InternetArchive.client
                .search(query, page = 1, rows = IA_SUMMARY_ROWS)
                .getOrNull()?.items.orEmpty()
                .map { it.toAlbumItem() }
            publishSummary()
        }

        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (audiusSummary == null) {
                        AudiusApi
                            .searchSummary(query)
                            .onSuccess {
                                audiusSummary =
                                    it.filterExplicit(
                                        context.dataStore.get(
                                            HideExplicitKey,
                                            false,
                                        ),
                                    )
                                publishSummary()
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else if (filter == AudiusApi.SearchFilter.FILTER_ALBUM) {
                    if (viewStateMap[filter.value] == null) loadAlbumsFirstPage()
                } else {
                    if (viewStateMap[filter.value] == null) {
                        AudiusApi
                            .search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false
                                                )
                                            ),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

                                                                                         
    private fun publishSummary() {
        val base = audiusSummary
        val ia = iaSummaryAlbums
        summaryPage = when {
            base != null -> mergeIaAlbums(base, ia)
            ia.isNotEmpty() -> SearchSummaryPage(listOf(SearchSummary(ALBUMS_TITLE, ia)))
            else -> null
        }
    }

    private fun mergeIaAlbums(page: SearchSummaryPage, iaAlbums: List<AlbumItem>): SearchSummaryPage {
        if (iaAlbums.isEmpty()) return page
        val sections = page.summaries.toMutableList()
        val albumsIndex = sections.indexOfFirst { it.title.equals(ALBUMS_TITLE, ignoreCase = true) }
        if (albumsIndex >= 0) {
            sections[albumsIndex] = sections[albumsIndex].copy(items = sections[albumsIndex].items + iaAlbums)
        } else {
            
            val listsIndex = sections.indexOfFirst { it.title.equals("Listas", ignoreCase = true) }
            sections.add(if (listsIndex >= 0) listsIndex else sections.size, SearchSummary(ALBUMS_TITLE, iaAlbums))
        }
        return SearchSummaryPage(sections)
    }

    private fun albumsContinuation(): String? =
        if (audiusAlbumsContinuation != null || iaNextPage != null) "more" else null

    private suspend fun loadAlbumsFirstPage() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val (audius, ia) = coroutineScope {
            val a = async { AudiusApi.search(query, AudiusApi.SearchFilter.FILTER_ALBUM).onFailure { reportException(it) }.getOrNull() }
            val i = async { InternetArchive.client.search(query, page = 1, rows = IA_ROWS).getOrNull() }
            a.await() to i.await()
        }
        
        if (audius == null && ia == null) return

        audiusAlbumsContinuation = audius?.continuation
        iaNextPage = if (ia != null && ia.items.size < ia.total) 2 else null
        viewStateMap[AudiusApi.SearchFilter.FILTER_ALBUM.value] =
            ItemsPage(
                (audius?.items.orEmpty() + ia?.items.orEmpty().map { it.toAlbumItem() })
                    .distinctBy { it.id }
                    .filterExplicit(hideExplicit),
                albumsContinuation(),
            )
    }

    private suspend fun loadMoreAlbums(viewState: ItemsPage) {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val audiusContinuation = audiusAlbumsContinuation
        val iaPage = iaNextPage
        val (audius, ia) = coroutineScope {
            val a = async { audiusContinuation?.let { AudiusApi.searchContinuation(it).getOrNull() } }
            val i = async { iaPage?.let { InternetArchive.client.search(query, page = it, rows = IA_ROWS).getOrNull() } }
            a.await() to i.await()
        }
        
        if (audius != null) audiusAlbumsContinuation = audius.continuation
        if (ia != null && iaPage != null) {
            iaNextPage = if (ia.items.isNotEmpty() && iaPage.toLong() * IA_ROWS < ia.total) iaPage + 1 else null
        }
        viewStateMap[AudiusApi.SearchFilter.FILTER_ALBUM.value] =
            ItemsPage(
                (viewState.items + audius?.items.orEmpty() + ia?.items.orEmpty().map { it.toAlbumItem() })
                    .distinctBy { it.id }
                    .filterExplicit(hideExplicit),
                albumsContinuation(),
            )
    }

    fun loadMore() {
        val filter = filter.value?.value ?: return
        if (loadingMore) return
        viewModelScope.launch {
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation ?: return@launch
            loadingMore = true
            try {
                if (filter == AudiusApi.SearchFilter.FILTER_ALBUM.value) {
                    loadMoreAlbums(viewState)
                } else {
                    val searchResult =
                        AudiusApi.searchContinuation(continuation).getOrNull() ?: return@launch
                    viewStateMap[filter] = ItemsPage(
                        (viewState.items + searchResult.items).distinctBy { it.id },
                        searchResult.continuation
                    )
                }
            } finally {
                loadingMore = false
            }
        }
    }

    private companion object {
        const val ALBUMS_TITLE = "Álbumes"
        const val IA_SUMMARY_ROWS = 8
        const val IA_ROWS = 30
    }
}