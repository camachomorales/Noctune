/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.internetarchive

                                                                                     
data class IaItem(
    val identifier: String,
    val title: String,
    val creator: String?,
    val downloads: Long,
    val thumbnailUrl: String,
                                                  
    val detailsUrl: String,
)

   
                                            
                                                                          
   
data class IaTrack(
    val id: String,
    val identifier: String,
    val fileName: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int?,
    val format: String,
    val streamUrl: String,
    val thumbnailUrl: String,
    val detailsUrl: String,
)

data class IaItemDetails(
    val item: IaItem,
    val description: String?,
                                                                                           
    val licenseUrl: String?,
    val tracks: List<IaTrack>,
)

data class IaSearchPage(
    val items: List<IaItem>,
    val total: Long,
    val page: Int,
)

                                                                                
data class IaTrackRef(
    val identifier: String,
    val fileName: String,
)
