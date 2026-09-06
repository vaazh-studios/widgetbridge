package com.vocabloot.widgetbridge

public enum class WidgetImageFormat(public val extension: String) { Png("png"), Jpeg("jpg") }

public class EncodedImage(public val bytes: ByteArray, public val extension: String)

/** Downsamples an image file for a widget. PNG keeps transparency (cutouts); JPEG for photos. */
public expect object WidgetImages {
    /** Long side at most [maxDimension], encoding at most [maxBytes] (shrinks by 3/4 steps down to 128 px). Null when unreadable or impossible. */
    public fun encode(sourcePath: String, format: WidgetImageFormat, maxDimension: Int = 512, maxBytes: Int = 2 * 1024 * 1024): EncodedImage?
}

public class AssetCandidate(public val fileName: String, public val sourcePath: String, public val format: WidgetImageFormat)

/** Greedy packer: encodes [AssetCandidate]s in order until [budgetBytes] is spent; failures are skipped. */
public class AssetBudget(
    private val budgetBytes: Long = 8L * 1024 * 1024,
    private val maxDimension: Int = 512,
    private val maxBytes: Int = 2 * 1024 * 1024,
    private val encoder: (candidate: AssetCandidate, maxDimension: Int, maxBytes: Int) -> EncodedImage? =
        { c, dim, bytes -> WidgetImages.encode(c.sourcePath, c.format, dim, bytes) },
) {
    public fun pack(candidates: List<AssetCandidate>): List<WidgetAsset> {
        val out = mutableListOf<WidgetAsset>()
        var used = 0L
        for (candidate in candidates) {
            val remaining = budgetBytes - used
            if (remaining <= 0L) break
            val cap = minOf(maxBytes.toLong(), remaining).toInt()
            val encoded = encoder(candidate, maxDimension, cap) ?: continue
            if (used + encoded.bytes.size > budgetBytes) continue
            out += WidgetAsset(fileName = candidate.fileName, bytes = encoded.bytes)
            used += encoded.bytes.size
        }
        return out
    }
}
