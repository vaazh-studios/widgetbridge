package com.vocabloot.widgetbridge

import com.vocabloot.widgetbridge.internal.FeedEnvelope
import com.vocabloot.widgetbridge.internal.FeedJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

public data class WidgetBridgeConfig(
    /** Bump when the payload shape changes incompatibly; readers on both platforms refuse other versions. */
    val schemaVersion: Int,
    /** Folder under the platform root: Android `<filesDir>/<name>`, iOS `<App Group container>/<name>`. */
    val directoryName: String = "widgetbridge",
    /** iOS only, required there: `group.com.example.app`. Ignored on Android. */
    val iosAppGroup: String? = null,
    /** Generations kept besides the current one, for fallback after a corrupt write. */
    val keepPrevious: Int = 1,
) {
    init {
        require(isSafeName(directoryName)) { "directoryName must be a safe name" }
        require(keepPrevious >= 0) { "keepPrevious must be >= 0" }
    }
}

public sealed interface PublishResult {
    /** The content fingerprint matched the current generation: nothing written, no redraw requested. */
    public data object Unchanged : PublishResult
    public data class Published(val generationId: String, val assetBytes: Long) : PublishResult
}

/** The generation a reader resolved: current, or the newest readable fallback. */
public class ResolvedFeed<T> internal constructor(
    public val payload: T,
    public val generationId: String,
    public val generatedAtEpochMs: Long,
    public val fingerprint: String,
    public val directory: String,
    private val storage: WidgetFeedStorage,
) {
    /** Absolute path of an asset shipped with this generation, or null when unsafe or missing. */
    public fun assetPath(fileName: String): String? = storage.assetPath(directory, fileName)
}

/**
 * Publishes a typed payload plus assets as an atomic generation and reads it back.
 * Construct through `androidWidgetBridge` / `iosWidgetBridge`, or directly with a custom storage.
 */
@OptIn(ExperimentalTime::class)
public class WidgetBridge<T>(
    private val config: WidgetBridgeConfig,
    private val serializer: KSerializer<T>,
    private val storage: WidgetFeedStorage,
    private val notifier: WidgetRefreshNotifier,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()

    /**
     * Writes a new generation unless the content is identical to the current one. Safe to call
     * from any dispatcher; concurrent calls are serialised. Throws [IllegalArgumentException]
     * for unsafe asset names before anything is written.
     */
    public suspend fun publish(payload: T, assets: List<WidgetAsset> = emptyList()): PublishResult = mutex.withLock {
        assets.forEach { require(isSafeName(it.fileName)) { "unsafe asset name: ${it.fileName}" } }
        val payloadJson = FeedJson.canonical.encodeToJsonElement(serializer, payload)
        val fingerprint = FeedJson.fingerprint(config.schemaVersion, payloadJson, assets)
        if (resolve()?.second?.fingerprint == fingerprint) return@withLock PublishResult.Unchanged

        val generatedAt = clock()
        val id = "$generatedAt-${fingerprint.take(8)}"
        val envelope = FeedEnvelope(config.schemaVersion, generatedAt, fingerprint, payloadJson)
        storage.writeGeneration(id, FeedJson.encodeEnvelope(envelope), assets, config.keepPrevious)
        notifier.notifyChanged()
        PublishResult.Published(generationId = id, assetBytes = assets.sumOf { it.bytes.size.toLong() })
    }

    /** The current generation, or the newest readable fallback with the same schema version; null when none. */
    public fun read(): ResolvedFeed<T>? {
        val (generation, envelope) = resolve() ?: return null
        val payload = runCatching { FeedJson.codec.decodeFromJsonElement(serializer, envelope.payload) }.getOrNull() ?: return null
        return ResolvedFeed(payload, generation.id, envelope.generatedAtEpochMs, envelope.fingerprint, generation.directoryPath, storage)
    }

    public fun clear(): Unit = storage.clear()

    private fun resolve(): Pair<WidgetGeneration, FeedEnvelope>? =
        storage.readGenerationCandidates().firstNotNullOfOrNull { generation ->
            runCatching { FeedJson.decodeEnvelope(generation.feedBytes) }.getOrNull()
                ?.takeIf { it.schemaVersion == config.schemaVersion }
                ?.let { generation to it }
        }
}
