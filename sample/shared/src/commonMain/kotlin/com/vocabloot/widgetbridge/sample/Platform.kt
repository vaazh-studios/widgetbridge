package com.vocabloot.widgetbridge.sample

import com.vocabloot.widgetbridge.WidgetBridge

expect fun createQuoteBridge(): WidgetBridge<QuoteFeed>

expect fun nowEpochMs(): Long

/** A writable directory for the bundled sample images; `WidgetImages.encode` reads from a file path. */
expect fun cacheDirectory(): String

expect fun ensureDirectory(path: String)

expect fun fileExists(path: String): Boolean

expect fun writeFile(path: String, bytes: ByteArray)

/** Bytes of a bundled cover image by file name. */
expect fun readBundledCover(name: String): ByteArray
