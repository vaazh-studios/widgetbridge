package com.vocabloot.widgetbridge.test

import com.vocabloot.widgetbridge.WidgetAsset
import com.vocabloot.widgetbridge.WidgetFeedStorage
import com.vocabloot.widgetbridge.WidgetGeneration
import com.vocabloot.widgetbridge.isSafeName

/**
 * In-memory [WidgetFeedStorage] honouring the candidate-order contract: generations are kept newest
 * first and pruned to `keepPrevious + 1`. Flip [prependCorruptGeneration] to put an unreadable
 * generation ahead of the real ones and test the reader's fallback.
 */
public class FakeWidgetFeedStorage : WidgetFeedStorage {
    /** Newest first, each with the assets written alongside it. */
    public val generations: ArrayDeque<Pair<WidgetGeneration, List<WidgetAsset>>> = ArrayDeque()
    public var writeCount: Int = 0
    public var lastKeepPrevious: Int = -1
    public var prependCorruptGeneration: Boolean = false

    override fun readGenerationCandidates(): List<WidgetGeneration> = buildList {
        if (prependCorruptGeneration) add(WidgetGeneration("corrupt", "/corrupt", "not-json".encodeToByteArray()))
        generations.forEach { add(it.first) }
    }

    override fun writeGeneration(generationId: String, feedBytes: ByteArray, assets: List<WidgetAsset>, keepPrevious: Int) {
        generations.addFirst(WidgetGeneration(generationId, "/$generationId", feedBytes) to assets)
        while (generations.size > keepPrevious + 1) generations.removeLast()
        writeCount += 1
        lastKeepPrevious = keepPrevious
    }

    override fun assetPath(generationDirectory: String, fileName: String): String? {
        if (!isSafeName(fileName)) return null
        val assets = generations.firstOrNull { it.first.directoryPath == generationDirectory }?.second ?: return null
        return assets.firstOrNull { it.fileName == fileName }?.let { "$generationDirectory/assets/$fileName" }
    }

    override fun clear() {
        generations.clear()
    }
}
