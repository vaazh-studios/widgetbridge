package com.vocabloot.widgetbridge.internal

import com.vocabloot.widgetbridge.WidgetAsset
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class FeedEnvelope(
    val schemaVersion: Int,
    val generatedAtEpochMs: Long,
    val fingerprint: String,
    val payload: JsonElement,
)

@Serializable
internal data class PointerFile(val current: String, val previous: List<String> = emptyList())

internal object FeedJson {
    /** On-disk codec: readable, tolerant of unknown keys added by newer versions. */
    val codec: Json = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = true; prettyPrint = true }

    /** Fingerprint codec: compact and deterministic. */
    val canonical: Json = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = true }

    fun encodeEnvelope(envelope: FeedEnvelope): ByteArray = codec.encodeToString(FeedEnvelope.serializer(), envelope).encodeToByteArray()
    fun decodeEnvelope(bytes: ByteArray): FeedEnvelope = codec.decodeFromString(FeedEnvelope.serializer(), bytes.decodeToString())
    fun encodePointer(pointer: PointerFile): ByteArray = codec.encodeToString(PointerFile.serializer(), pointer).encodeToByteArray()
    fun decodePointer(bytes: ByteArray): PointerFile = codec.decodeFromString(PointerFile.serializer(), bytes.decodeToString())

    /** sha256 over schema version, canonical payload JSON, and each asset's name, size and content hash (sorted by name). */
    fun fingerprint(schemaVersion: Int, payload: JsonElement, assets: List<WidgetAsset>): String {
        val text = buildString {
            append(schemaVersion).append('\n')
            append(canonical.encodeToString(JsonElement.serializer(), payload)).append('\n')
            assets.sortedBy { it.fileName }.forEach { asset ->
                append(asset.fileName).append('').append(asset.bytes.size).append('').append(sha256Hex(asset.bytes)).append('\n')
            }
        }
        return sha256Hex(text.encodeToByteArray())
    }
}
