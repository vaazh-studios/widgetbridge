package com.vocabloot.widgetbridge.sample

import kotlinx.serialization.Serializable

@Serializable
data class Quote(val id: String, val text: String, val author: String, val image: String? = null)

/** Everything the widgets need. A real app passes localised strings here; the extension has no resources. */
@Serializable
data class QuoteFeed(
    val quotes: List<Quote>,
    val emptyTitle: String,
    val emptyBody: String,
    val featuredId: String? = null,
    val featuredUntilEpochMs: Long? = null,
)

const val QUOTE_SCHEMA_VERSION = 1
