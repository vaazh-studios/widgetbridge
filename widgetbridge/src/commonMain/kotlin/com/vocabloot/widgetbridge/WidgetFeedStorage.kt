package com.vocabloot.widgetbridge

/** One exported generation as found on disk. */
public class WidgetGeneration(
    public val id: String,
    public val directoryPath: String,
    public val feedBytes: ByteArray,
)

/** An image (or any file) shipped inside a generation under `assets/`. [fileName] must satisfy [isSafeName]. */
public class WidgetAsset(public val fileName: String, public val bytes: ByteArray)

/** Letters, digits, `-`, `_`, `.`; never blank. Rejects path separators and traversal by construction. */
public fun isSafeName(value: String): Boolean =
    value.isNotBlank() && value.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }

/**
 * Platform filesystem operations for the generation-versioned export. Implement it yourself
 * for a custom location; the defaults are `AndroidWidgetFeedStorage` and `IosWidgetFeedStorage`.
 *
 * Contract for [readGenerationCandidates] order: pointer `current`, then pointer `previous` in
 * order, then every other non-temp generation directory sorted descending by id. Unsafe ids and
 * directories without `feed.json` are skipped. Never throws for corrupt files.
 */
public interface WidgetFeedStorage {
    public fun readGenerationCandidates(): List<WidgetGeneration>

    /** Writes a complete generation, promotes it atomically, updates the pointer, prunes to [keepPrevious] old ones. */
    public fun writeGeneration(generationId: String, feedBytes: ByteArray, assets: List<WidgetAsset>, keepPrevious: Int)

    /** Absolute path of `assets/<fileName>` inside [generationDirectory], or null when unsafe or missing. */
    public fun assetPath(generationDirectory: String, fileName: String): String?

    /** Removes every generation and the pointer. */
    public fun clear()
}
