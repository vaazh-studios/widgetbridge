@file:OptIn(ExperimentalForeignApi::class)

package com.vocabloot.widgetbridge.sample

import com.vocabloot.widgetbridge.WidgetBridge
import com.vocabloot.widgetbridge.WidgetBridgeConfig
import com.vocabloot.widgetbridge.iosWidgetBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.NSDate
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970

const val SAMPLE_APP_GROUP = "group.com.vocabloot.widgetbridge.sample"

actual fun createQuoteBridge(): WidgetBridge<QuoteFeed> = iosWidgetBridge(
    config = WidgetBridgeConfig(schemaVersion = QUOTE_SCHEMA_VERSION, iosAppGroup = SAMPLE_APP_GROUP),
    serializer = QuoteFeed.serializer(),
)

actual fun nowEpochMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun cacheDirectory(): String = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String

actual fun ensureDirectory(path: String) {
    NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
}

actual fun fileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class)
actual fun writeFile(path: String, bytes: ByteArray) {
    val data = if (bytes.isEmpty()) NSData() else bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    data.writeToFile(path, atomically = true)
}

@OptIn(ExperimentalForeignApi::class)
actual fun readBundledCover(name: String): ByteArray {
    val path = NSBundle.mainBundle.pathForResource(name.substringBeforeLast('.'), name.substringAfterLast('.'), "Covers")
        ?: NSBundle.mainBundle.pathForResource(name.substringBeforeLast('.'), name.substringAfterLast('.'))
        ?: error("missing bundled cover $name")
    val data = NSData.dataWithContentsOfFile(path) ?: error("unreadable bundled cover $name")
    return data.bytes?.readBytes(data.length.toInt()) ?: ByteArray(0)
}
