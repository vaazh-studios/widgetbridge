package com.vocabloot.widgetbridge

import android.content.Context
import com.vocabloot.widgetbridge.internal.FeedJson
import com.vocabloot.widgetbridge.internal.PointerFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Generation folders under [root]; every promote and pointer write is an atomic rename. */
public class AndroidWidgetFeedStorage(private val root: File) : WidgetFeedStorage {

    private val generations = File(root, GENERATIONS_DIRECTORY)

    init { generations.mkdirs() }

    override fun readGenerationCandidates(): List<WidgetGeneration> {
        val pointer = runCatching { pointerFile().takeIf(File::exists)?.readBytes()?.let(FeedJson::decodePointer) }.getOrNull()
        val orderedIds = buildList {
            pointer?.current?.let(::add)
            pointer?.previous?.forEach(::add)
            generations.listFiles()?.asSequence()?.filter(File::isDirectory)?.map(File::getName)
                ?.filterNot { it.endsWith(TEMP_SUFFIX) }?.sortedDescending()?.forEach(::add)
        }.distinct()
        return orderedIds.mapNotNull { id ->
            if (!isSafeName(id)) return@mapNotNull null
            val directory = File(generations, id)
            val feed = File(directory, FEED_FILE)
            if (!feed.isFile) return@mapNotNull null
            runCatching { WidgetGeneration(id = id, directoryPath = directory.absolutePath, feedBytes = feed.readBytes()) }.getOrNull()
        }
    }

    override fun writeGeneration(generationId: String, feedBytes: ByteArray, assets: List<WidgetAsset>, keepPrevious: Int) {
        require(isSafeName(generationId)) { "Unsafe generation id" }
        val temporary = File(generations, generationId + TEMP_SUFFIX)
        val target = File(generations, generationId)
        temporary.deleteRecursively()
        val assetDirectory = File(temporary, ASSETS_DIRECTORY).also(File::mkdirs)
        assets.forEach { asset ->
            require(isSafeName(asset.fileName)) { "Unsafe asset name" }
            File(assetDirectory, asset.fileName).writeBytes(asset.bytes)
        }
        File(temporary, FEED_FILE).writeBytes(feedBytes)
        target.deleteRecursively()
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)

        val oldPointer = runCatching { pointerFile().takeIf(File::exists)?.readBytes()?.let(FeedJson::decodePointer) }.getOrNull()
        val previous = listOfNotNull(oldPointer?.current).plus(oldPointer?.previous.orEmpty())
            .filter { it != generationId }.distinct().take(keepPrevious)
        val next = PointerFile(current = generationId, previous = previous)
        atomicWrite(pointerFile(), FeedJson.encodePointer(next))
        val retained = (previous + generationId).toSet()
        generations.listFiles()?.filter { it.name !in retained }?.forEach(File::deleteRecursively)
    }

    override fun assetPath(generationDirectory: String, fileName: String): String? {
        if (!isSafeName(fileName)) return null
        val directory = File(generationDirectory).canonicalFile
        val candidate = File(File(directory, ASSETS_DIRECTORY), fileName).canonicalFile
        return candidate.takeIf { it.isFile && it.path.startsWith(directory.path + File.separator) }?.absolutePath
    }

    override fun clear() {
        generations.deleteRecursively()
        pointerFile().delete()
        generations.mkdirs()
    }

    private fun atomicWrite(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val temporary = File(file.absolutePath + TEMP_SUFFIX)
        FileOutputStream(temporary).use { output -> output.write(bytes); output.flush(); output.fd.sync() }
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun pointerFile() = File(root, POINTER_FILE)

    public companion object {
        /** `<filesDir>/<directoryName>`. */
        public fun forApp(context: Context, directoryName: String): AndroidWidgetFeedStorage =
            AndroidWidgetFeedStorage(File(context.filesDir, directoryName))

        private const val GENERATIONS_DIRECTORY = "generations"
        private const val ASSETS_DIRECTORY = "assets"
        private const val FEED_FILE = "feed.json"
        private const val POINTER_FILE = "current.json"
        private const val TEMP_SUFFIX = ".tmp"
    }
}
