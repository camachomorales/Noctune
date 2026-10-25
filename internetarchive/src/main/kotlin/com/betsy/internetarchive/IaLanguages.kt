/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.betsy.internetarchive

   
                                                                                                 
                                                                              
                                                                       
   
internal object IaLanguages {

    private val terms: Map<String, List<String>> = mapOf(
        "af" to listOf("afr", "Afrikaans"),
        "az" to listOf("aze", "Azerbaijani"),
        "id" to listOf("ind", "Indonesian"),
        "ms" to listOf("msa", "may", "Malay"),
        "ca" to listOf("cat", "Catalan"),
        "cs" to listOf("ces", "cze", "Czech"),
        "da" to listOf("dan", "Danish"),
        "de" to listOf("deu", "ger", "German"),
        "et" to listOf("est", "Estonian"),
        "en" to listOf("eng", "English"),
        "es" to listOf("spa", "Spanish"),
        "eu" to listOf("eus", "baq", "Basque"),
        "fil" to listOf("fil", "tgl", "Filipino", "Tagalog"),
        "fr" to listOf("fra", "fre", "French"),
        "gl" to listOf("glg", "Galician"),
        "hr" to listOf("hrv", "Croatian"),
        "zu" to listOf("zul", "Zulu"),
        "is" to listOf("isl", "ice", "Icelandic"),
        "it" to listOf("ita", "Italian"),
        "sw" to listOf("swa", "Swahili"),
        "lt" to listOf("lit", "Lithuanian"),
        "hu" to listOf("hun", "Hungarian"),
        "nl" to listOf("nld", "dut", "Dutch"),
        "no" to listOf("nor", "nob", "Norwegian"),
        "or" to listOf("ori", "Oriya", "Odia"),
        "uz" to listOf("uzb", "Uzbek"),
        "pl" to listOf("pol", "Polish"),
        "pt" to listOf("por", "Portuguese"),
        "ro" to listOf("ron", "rum", "Romanian"),
        "sq" to listOf("sqi", "alb", "Albanian"),
        "sk" to listOf("slk", "slo", "Slovak"),
        "sl" to listOf("slv", "Slovenian"),
        "fi" to listOf("fin", "Finnish"),
        "sv" to listOf("swe", "Swedish"),
        "bo" to listOf("bod", "tib", "Tibetan"),
        "vi" to listOf("vie", "Vietnamese"),
        "tr" to listOf("tur", "Turkish"),
        "bg" to listOf("bul", "Bulgarian"),
        "ky" to listOf("kir", "Kyrgyz"),
        "kk" to listOf("kaz", "Kazakh"),
        "mk" to listOf("mkd", "mac", "Macedonian"),
        "mn" to listOf("mon", "Mongolian"),
        "ru" to listOf("rus", "Russian"),
        "sr" to listOf("srp", "Serbian"),
        "uk" to listOf("ukr", "Ukrainian"),
        "el" to listOf("ell", "gre", "Greek"),
        "hy" to listOf("hye", "arm", "Armenian"),
        "iw" to listOf("heb", "Hebrew"),
        "he" to listOf("heb", "Hebrew"),
        "ur" to listOf("urd", "Urdu"),
        "ar" to listOf("ara", "Arabic"),
        "fa" to listOf("fas", "per", "Persian"),
        "ne" to listOf("nep", "Nepali"),
        "mr" to listOf("mar", "Marathi"),
        "hi" to listOf("hin", "Hindi"),
        "bn" to listOf("ben", "Bengali"),
        "pa" to listOf("pan", "Punjabi"),
        "gu" to listOf("guj", "Gujarati"),
        "ta" to listOf("tam", "Tamil"),
        "te" to listOf("tel", "Telugu"),
        "kn" to listOf("kan", "Kannada"),
        "ml" to listOf("mal", "Malayalam"),
        "si" to listOf("sin", "Sinhala"),
        "th" to listOf("tha", "Thai"),
        "lo" to listOf("lao", "Lao"),
        "my" to listOf("mya", "bur", "Burmese"),
        "ka" to listOf("kat", "geo", "Georgian"),
        "am" to listOf("amh", "Amharic"),
        "km" to listOf("khm", "Khmer"),
        "zh" to listOf("zho", "chi", "Chinese"),
        "ja" to listOf("jpn", "Japanese"),
        "ko" to listOf("kor", "Korean"),
    )

       
                                                                                      
                                                                                     
       
    fun clauseFor(appLanguage: String?): String? {
        val base = appLanguage?.substringBefore('-')?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        val list = terms[base] ?: return null
        return "language:(" + list.distinct().joinToString(" OR ") + ")"
    }
}
