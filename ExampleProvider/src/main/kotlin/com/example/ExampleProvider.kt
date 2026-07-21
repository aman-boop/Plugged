package com.example

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType

class ExampleProvider : MainAPI() { // All providers must be an instance of MainAPI
    override var mainUrl = "https://animexin.dev/" 
    override var name = "animexin"
    override val supportedTypes = setOf(TvType.Movie)

    override var lang = "en"

    // Enable this when your provider has a main page
    override val hasMainPage = false

    // This function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {

        val document = app.get(mainUrl + "search?keyword=$query").document

        return document.select("div.bs").mapNotNull { element ->
            val title = element.selectFirst("h2")?.text() ?: return@mapNotNull null
            val url = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = element.selectFirst("img")?.attr("src") ?: return@mapNotNull null

            SearchResponse(
                name = title,
                url = url,
                posterUrl = posterUrl
            )

        }
    }
}