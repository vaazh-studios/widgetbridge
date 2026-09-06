package com.vocabloot.widgetbridge

/** In-memory storage honouring the candidate-order contract. Generations are kept in write order, newest first. */
class FakeWidgetFeedStorage : WidgetFeedStorage {
    val generations = ArrayDeque<Pair<WidgetGeneration, List<WidgetAsset>>>()
    var writeCount = 0
    var lastKeepPrevious = -1
    var prependCorruptGeneration = false

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

    override fun clear() = generations.clear()
}
