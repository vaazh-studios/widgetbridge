package com.vocabloot.widgetbridge.sample

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** [image] is the asset file name inside the published generation; [cover] picks the bundled source image and never leaves the app. */
@Serializable
data class Quote(val id: String, val text: String, val author: String, val image: String? = null, @Transient val cover: Int = 0)

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
