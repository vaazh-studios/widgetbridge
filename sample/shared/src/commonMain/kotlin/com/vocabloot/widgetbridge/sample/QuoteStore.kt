package com.vocabloot.widgetbridge.sample

import com.vocabloot.widgetbridge.AssetBudget
import com.vocabloot.widgetbridge.AssetCandidate
import com.vocabloot.widgetbridge.WidgetAsset
import com.vocabloot.widgetbridge.WidgetImageFormat
import com.vocabloot.widgetbridge.WidgetRotation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-memory data for the sample; the feed and its assets are derived from it. */
class QuoteStore {
    private val _quotes = MutableStateFlow(
        listOf(
            Quote("q1", "Simplicity is the soul of efficiency.", "Austin Freeman", image = "q1.jpg", cover = 0),
            Quote("q2", "Make it work, make it right, make it fast.", "Kent Beck", image = "q2.jpg", cover = 1),
            Quote("q3", "Programs must be written for people to read.", "Harold Abelson", image = "q3.jpg", cover = 2),
            Quote("q4", "The best way to predict the future is to invent it.", "Alan Kay", image = "q4.jpg", cover = 3),
            Quote("q5", "Talk is cheap. Show me the code.", "Linus Torvalds", image = "q5.jpg", cover = 4),
        ),
    )
    private val _featured = MutableStateFlow<Pair<String, Long>?>(null)

    /** Source image files on disk, one per cover; empty until [attachImages]. */
    private val _sources = MutableStateFlow<List<String>>(emptyList())

    val quotes: StateFlow<List<Quote>> = _quotes
    val featured: StateFlow<Pair<String, Long>?> = _featured
    val sources: StateFlow<List<String>> = _sources

    fun attachImages(paths: List<String>) { _sources.value = paths }

    fun add(text: String, author: String) {
        val id = "q${nowEpochMs()}"
        val cover = WidgetRotation.index(WidgetRotation.stableHash(id).toLong(), SampleImages.COUNT)
        _quotes.update { it + Quote(id = id, text = text.trim(), author = author.trim().ifEmpty { "Unknown" }, image = "$id.jpg", cover = cover) }
    }

    fun remove(id: String) = _quotes.update { list -> list.filterNot { it.id == id } }

    fun feature(id: String, untilEpochMs: Long) { _featured.value = id to untilEpochMs }

    fun feed(): QuoteFeed = QuoteFeed(
        quotes = _quotes.value,
        emptyTitle = "Quote of the day",
        emptyBody = "Add a quote in the app",
        featuredId = _featured.value?.first,
        featuredUntilEpochMs = _featured.value?.second,
    )

    /**
     * Downsampled JPEGs for every quote that has a cover, in feed order, within the default 8 MB budget.
     * Decodes images: call it off the main thread.
     */
    fun assets(): List<WidgetAsset> {
        val sources = _sources.value
        if (sources.isEmpty()) return emptyList()
        val candidates = _quotes.value.mapNotNull { quote ->
            quote.image?.let { AssetCandidate(fileName = it, sourcePath = sources[quote.cover % sources.size], format = WidgetImageFormat.Jpeg) }
        }
        return AssetBudget(maxDimension = 256).pack(candidates)
    }
}
