/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.internetarchive

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

   
                                                                      
                                                  
                                                        
                                                                      
                                     
  
                                                                        
   
class InternetArchiveClient(
    private val userAgent: String = "BetsyMusic/1.0 (Android; +https://github.com/camachomorales/Noctune)",
    private val baseUrl: String = "https://archive.org",
) {
    private val http = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun getJson(path: String, params: List<Pair<String, String>> = emptyList()): JsonObject {
        val response = http.get("$baseUrl$path") {
            header(HttpHeaders.UserAgent, userAgent)
            url { params.forEach { (k, v) -> parameters.append(k, v) } }
        }
        if (!response.status.isSuccess()) error("archive.org respondió ${response.status.value} en $path")
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    

       
                                                                                 
                                                                                                   
                                                                                         
                                                                                     
       
    @Volatile
    var contentLanguage: String? = null

    private val languageFallback: MutableSet<String> = ConcurrentHashMap.newKeySet()

                                                     
    suspend fun search(query: String, page: Int = 1, rows: Int = 30): Result<IaSearchPage> = runCatching {
        val words = sanitize(query)
        require(words.isNotEmpty()) { "Búsqueda vacía" }
        val base = "(${words.joinToString(" AND ")}) AND mediatype:audio"

        val language = contentLanguage
        val clause = IaLanguages.clauseFor(language)
        if (clause == null) return@runCatching runSearch(base, page, rows)

        val key = "$language|${words.joinToString(" ").lowercase()}"
        if (key in languageFallback) return@runCatching runSearch(base, page, rows)

        val filtered = runSearch("$base AND $clause", page, rows)
        if (page == 1 && filtered.total < MIN_LANGUAGE_RESULTS) {
            if (languageFallback.size > 200) languageFallback.clear()
            languageFallback.add(key)
            return@runCatching runSearch(base, page, rows)
        }
        filtered
    }

    private suspend fun runSearch(q: String, page: Int, rows: Int): IaSearchPage {
        val root = getJson(
            "/advancedsearch.php",
            listOf(
                "q" to q,
                "fl[]" to "identifier",
                "fl[]" to "title",
                "fl[]" to "creator",
                "fl[]" to "downloads",
                "sort[]" to "downloads desc",
                "rows" to rows.toString(),
                "page" to page.toString(),
                "output" to "json",
            ),
        )
        val response = root["response"]?.jsonObject
        val docs = response?.get("docs")?.jsonArray ?: JsonArray(emptyList())
        val items = docs.mapNotNull { doc ->
            val o = doc as? JsonObject ?: return@mapNotNull null
            val id = o["identifier"].firstString() ?: return@mapNotNull null
            IaItem(
                identifier = id,
                title = o["title"].firstString() ?: id,
                creator = o["creator"].firstString(),
                downloads = o["downloads"].firstString()?.toLongOrNull() ?: 0L,
                thumbnailUrl = thumbnailUrl(id),
                detailsUrl = detailsUrl(id),
            )
        }
        return IaSearchPage(
            items = items,
            total = response?.get("numFound").firstString()?.toLongOrNull() ?: items.size.toLong(),
            page = page,
        )
    }

    

                                                                                                     
    suspend fun details(identifier: String): Result<IaItemDetails> = runCatching {
        val root = getJson("/metadata/${encodeSegment(identifier)}")
        val meta = root["metadata"]?.jsonObject ?: error("Item sin metadatos")
        val itemTitle = meta["title"].firstString() ?: identifier
        val itemCreator = meta["creator"].firstString()
        val thumb = thumbnailUrl(identifier)
        val details = detailsUrl(identifier)

        val files = (root["files"]?.jsonArray ?: JsonArray(emptyList())).mapNotNull { it as? JsonObject }

        class Candidate(val name: String, val obj: JsonObject, val rank: Int)

        
        val groups = LinkedHashMap<String, MutableList<Candidate>>()
        for (f in files) {
            val name = f["name"].firstString() ?: continue
            val rank = playRank(name, f["format"].firstString()) ?: continue
            if (f["private"].firstString() == "true") continue
            val key = f["original"].firstString() ?: name
            groups.getOrPut(key) { mutableListOf() }.add(Candidate(name, f, rank))
        }

        val tracks = groups.values.map { group ->
            val best = group.minByOrNull { it.rank }!!
            
            fun field(key: String) = (listOf(best) + group).firstNotNullOfOrNull { it.obj[key].firstString() }
            val trackNo = field("track")?.takeWhile { it.isDigit() }?.toIntOrNull()
            Triple(
                trackNo,
                best.name,
                IaTrack(
                    id = "$ID_PREFIX$identifier/${best.name}",
                    identifier = identifier,
                    fileName = best.name,
                    title = field("title") ?: best.name.substringBeforeLast('.'),
                    artist = field("creator") ?: itemCreator ?: "Internet Archive",
                    album = field("album") ?: itemTitle,
                    durationSeconds = parseLength(field("length")),
                    format = best.obj["format"].firstString() ?: best.name.substringAfterLast('.'),
                    streamUrl = downloadUrl(identifier, best.name),
                    thumbnailUrl = thumb,
                    detailsUrl = details,
                ),
            )
        }.sortedWith(compareBy({ it.first ?: Int.MAX_VALUE }, { it.second.lowercase() }))
            .map { it.third }

        IaItemDetails(
            item = IaItem(identifier, itemTitle, itemCreator, 0L, thumb, details),
            description = meta["description"].firstString()?.let(::stripHtml)?.takeIf { it.isNotBlank() },
            licenseUrl = meta["licenseurl"].firstString(),
            tracks = tracks,
        )
    }

    

    fun downloadUrl(identifier: String, fileName: String): String =
        "$baseUrl/download/${encodeSegment(identifier)}/${fileName.split('/').joinToString("/") { encodeSegment(it) }}"

    fun thumbnailUrl(identifier: String): String = "$baseUrl/services/img/${encodeSegment(identifier)}"

    fun detailsUrl(identifier: String): String = "$baseUrl/details/${encodeSegment(identifier)}"

    

    private fun encodeSegment(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

                                                                               
    internal fun sanitize(query: String): List<String> =
        query.replace(Regex("[+\\-!(){}\\[\\]^\"~*?:\\\\/&|]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.uppercase() !in setOf("AND", "OR", "NOT", "TO") }

                                                                                 
    private fun playRank(name: String, format: String?): Int? {
        val ext = name.substringAfterLast('.', "").lowercase()
        val fmt = format.orEmpty().lowercase()
        return when (ext) {
            "mp3" -> if ("vbr" in fmt) 0 else if ("128" in fmt) 1 else 2
            "ogg", "oga" -> 3
            "opus" -> 4
            "m4a", "aac" -> 5
            "flac" -> 6
            "wav" -> 7
            else -> null
        }
    }

                                                   
    private fun parseLength(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        if (':' in raw) {
            val parts = raw.split(':').mapNotNull { it.toDoubleOrNull() }
            if (parts.size != raw.split(':').size) return null
            return parts.fold(0.0) { acc, p -> acc * 60 + p }.toInt()
        }
        return raw.toDoubleOrNull()?.toInt()
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'")
            .replace(Regex("[ \\t]+"), " ")
            .lines().joinToString("\n") { it.trim() }
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

                                                                                              
    private fun JsonElement?.firstString(): String? = when (this) {
        null -> null
        is JsonPrimitive -> content.takeIf { it.isNotBlank() }
        is JsonArray -> firstNotNullOfOrNull { it.firstString() }
        else -> null
    }

    companion object {
                                                                                                        
        const val MIN_LANGUAGE_RESULTS = 6

        const val ID_PREFIX = "ia:"
    }
}