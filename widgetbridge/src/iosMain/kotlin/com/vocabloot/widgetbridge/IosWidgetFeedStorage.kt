package com.vocabloot.widgetbridge

import com.vocabloot.widgetbridge.internal.FeedJson
import com.vocabloot.widgetbridge.internal.PointerFile
import com.vocabloot.widgetbridge.internal.toByteArray
import com.vocabloot.widgetbridge.internal.toNSData
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.writeToFile

/** Generation folders inside the App Group container so the widget extension can read them. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class IosWidgetFeedStorage internal constructor(private val rootProvider: () -> String) : WidgetFeedStorage {

    /** `<App Group container>/<directoryName>`. Throws on first use when the App Group is not in the entitlements. */
    public constructor(appGroup: String, directoryName: String) : this({
        val container = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(appGroup)
            ?: error("App Group $appGroup is not available; add it to the app and widget entitlements")
        "${container.path}/$directoryName"
    })

    /** Tests and custom locations: an explicit absolute root path. */
    internal constructor(rootPath: String) : this({ rootPath })

    private val fileManager = NSFileManager.defaultManager
    private val root: String by lazy { rootProvider().also { createDirectory(it); createDirectory("$it/$GENERATIONS_DIRECTORY") } }

    override fun readGenerationCandidates(): List<WidgetGeneration> {
        val pointer = runCatching { readBytes("$root/$POINTER_FILE")?.let(FeedJson::decodePointer) }.getOrNull()
        val generationsPath = "$root/$GENERATIONS_DIRECTORY"
        val names = fileManager.contentsOfDirectoryAtPath(generationsPath, null)?.filterIsInstance<String>().orEmpty()
            .filterNot { it.endsWith(TEMP_SUFFIX) }.sortedDescending()
        val orderedIds = buildList { pointer?.current?.let(::add); pointer?.previous?.forEach(::add); addAll(names) }.distinct()
        return orderedIds.mapNotNull { id ->
            if (!isSafeName(id)) return@mapNotNull null
            val directory = "$generationsPath/$id"
            val feed = readBytes("$directory/$FEED_FILE") ?: return@mapNotNull null
            WidgetGeneration(id = id, directoryPath = directory, feedBytes = feed)
        }
    }

    override fun writeGeneration(generationId: String, feedBytes: ByteArray, assets: List<WidgetAsset>, keepPrevious: Int) {
        require(isSafeName(generationId)) { "Unsafe generation id" }
        val generationsPath = "$root/$GENERATIONS_DIRECTORY"
        val temporary = "$generationsPath/$generationId$TEMP_SUFFIX"
        val target = "$generationsPath/$generationId"
        remove(temporary)
        createDirectory("$temporary/$ASSETS_DIRECTORY")
        assets.forEach { asset ->
            require(isSafeName(asset.fileName)) { "Unsafe asset name" }
            writeBytes("$temporary/$ASSETS_DIRECTORY/${asset.fileName}", asset.bytes)
        }
        writeBytes("$temporary/$FEED_FILE", feedBytes)
        remove(target)
        if (!fileManager.moveItemAtPath(temporary, target, null)) error("Could not promote generation $generationId")

        val old = runCatching { readBytes("$root/$POINTER_FILE")?.let(FeedJson::decodePointer) }.getOrNull()
        val previous = listOfNotNull(old?.current).plus(old?.previous.orEmpty()).filter { it != generationId }.distinct().take(keepPrevious)
        writeBytes("$root/$POINTER_FILE", FeedJson.encodePointer(PointerFile(current = generationId, previous = previous)))
        val retained = (previous + generationId).toSet()
        fileManager.contentsOfDirectoryAtPath(generationsPath, null)?.filterIsInstance<String>().orEmpty()
            .filterNot { it in retained }.forEach { remove("$generationsPath/$it") }
    }

    override fun assetPath(generationDirectory: String, fileName: String): String? {
        if (!isSafeName(fileName)) return null
        val directory = NSURL.fileURLWithPath(generationDirectory).standardizedURL?.path ?: return null
        val candidate = NSURL.fileURLWithPath("$directory/$ASSETS_DIRECTORY/$fileName").standardizedURL?.path ?: return null
        return candidate.takeIf { it.startsWith("$directory/") && fileManager.fileExistsAtPath(it) }
    }

    override fun clear() {
        remove("$root/$GENERATIONS_DIRECTORY")
        remove("$root/$POINTER_FILE")
        createDirectory("$root/$GENERATIONS_DIRECTORY")
    }

    private fun createDirectory(path: String) {
        if (!fileManager.fileExistsAtPath(path) && !fileManager.createDirectoryAtPath(path, true, null, null)) error("Could not create $path")
    }
    private fun readBytes(path: String): ByteArray? = fileManager.contentsAtPath(path)?.toByteArray()
    private fun writeBytes(path: String, bytes: ByteArray) {
        createDirectory(path.substringBeforeLast('/'))
        if (!bytes.toNSData().writeToFile(path, atomically = true)) error("Could not write $path")
    }
    private fun remove(path: String) { if (fileManager.fileExistsAtPath(path)) fileManager.removeItemAtPath(path, null) }

    private companion object {
        const val GENERATIONS_DIRECTORY = "generations"
        const val ASSETS_DIRECTORY = "assets"
        const val FEED_FILE = "feed.json"
        const val POINTER_FILE = "current.json"
        const val TEMP_SUFFIX = ".tmp"
    }
}
