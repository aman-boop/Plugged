package com.example

import com.lagradost.cloudstream3.*
// import com.lagradost.cloudstream3.SearchResponse
// import com.lagradost.cloudstream3.TvType

class ExampleProvider : MainAPI() { // All providers must be an instance of MainAPI
    override var mainUrl = "https://animexin.dev/" 
    override var name = "animexin"
    override val supportedTypes = setOf(TvType.Anime)

    override var lang = "en"

    // Enable this when your provider has a main page
    override val hasMainPage = false

    // This function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        var document = app.get("$mainUrl?s=$query").document

        return document.select("article.bs").mapNotNull { element ->

            val title = element.select("h2").text()
            if (title.isEmpty()) return@mapNotNull null

            val href = fixUrl(element.select("a").attr("href"))
            if (href.isEmpty()) return@mapNotNull null

            val posterUrl = fixUrl(element.select("img").attr("src"))
            if (posterUrl.isEmpty()) return@mapNotNull null

            listOf(
                newTvSeriesSearchResponse(title, href, TvType.Anime) {
                    this.posterUrl = posterUrl
                }
            )   
        }.flatten()
    }

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    // 1. Parse basic details from the show's page
    val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
    val poster = fixUrl(document.selectFirst("div.thumb img")?.attr("src"))
    val description = document.selectFirst("div.entry-content p")?.text()?.trim()
    val genres = document.select("div.genxrel a").map { it.text() }
    
    // Status (e.g., Ongoing, Completed)
    val status = when (document.selectFirst("div.info-content span")?.text()?.contains("Completed") == true) {
        true -> TrabalStatus.Completed
        else -> TrabalStatus.Ongoing
    }

    // 2. Parse episodes list (usually found inside a list/selector on the page)
    val episodes = document.select("div.episodelist ul li").mapNotNull { element ->
        val episodeHref = fixUrl(element.select("a").attr("href"))
        val episodeName = element.select("span.eps").text()
        // Extract a clean episode number if possible, or leave null to auto-index
        val episodeNumber = Regex("""\d+""").find(episodeName)?.value?.toIntOrNull()

        Episode(
            data = episodeHref,
            name = episodeName,
            episode = episodeNumber
        )
    }.reversed() // Reverse if the newest episodes are at the top, so it plays in order

    // 3. Return the proper LoadResponse type (e.g., AnimeLoadResponse)
    return newMovieLoadResponse(title, url, TvType.Anime, episodes) {
        this.posterUrl = poster
        this.plot = description
        this.tags = genres
        this.showStatus = status
    }
    }
}
