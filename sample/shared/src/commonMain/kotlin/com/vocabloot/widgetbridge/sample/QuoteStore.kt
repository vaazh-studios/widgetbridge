package com.vocabloot.widgetbridge.sample

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-memory data for the sample; the feed is derived from it. */
class QuoteStore {
    private val _quotes = MutableStateFlow(
        listOf(
            Quote("q1", "Simplicity is the soul of efficiency.", "Austin Freeman"),
            Quote("q2", "Make it work, make it right, make it fast.", "Kent Beck"),
            Quote("q3", "Programs must be written for people to read.", "Harold Abelson"),
            Quote("q4", "The best way to predict the future is to invent it.", "Alan Kay"),
            Quote("q5", "Talk is cheap. Show me the code.", "Linus Torvalds"),
        ),
    )
    private val _featured = MutableStateFlow<Pair<String, Long>?>(null)

    val quotes: StateFlow<List<Quote>> = _quotes
    val featured: StateFlow<Pair<String, Long>?> = _featured

    fun add(text: String, author: String) {
        _quotes.update { it + Quote(id = "q${nowEpochMs()}", text = text.trim(), author = author.trim().ifEmpty { "Unknown" }) }
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
}
