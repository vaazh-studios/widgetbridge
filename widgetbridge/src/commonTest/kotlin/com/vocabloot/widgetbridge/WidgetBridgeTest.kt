package com.vocabloot.widgetbridge

import com.vocabloot.widgetbridge.test.CountingNotifier
import com.vocabloot.widgetbridge.test.FakeWidgetFeedStorage

import com.vocabloot.widgetbridge.internal.FeedJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetBridgeTest {

    @Serializable
    data class Quotes(val quotes: List<String>, val emptyText: String = "Nothing yet", val image: String? = null)

    private class Harness(schemaVersion: Int = 1, keepPrevious: Int = 1) {
        val storage = FakeWidgetFeedStorage()
        val notifier = CountingNotifier()
        var now = 1_000L
        val bridge = WidgetBridge(
            config = WidgetBridgeConfig(schemaVersion = schemaVersion, keepPrevious = keepPrevious),
            serializer = Quotes.serializer(),
            storage = storage,
            notifier = notifier,
            clock = { now },
        )
    }

    @Test
    fun first_publish_writes_a_generation_and_notifies() = runTest {
        val h = Harness()

        val result = h.bridge.publish(Quotes(listOf("a")))

        assertIs<PublishResult.Published>(result)
        assertTrue(result.generationId.startsWith("1000-"))
        assertEquals(1, h.storage.writeCount)
        assertEquals(1, h.notifier.count)
        assertEquals(1, h.storage.lastKeepPrevious)
    }

    @Test
    fun identical_content_skips_a_second_generation_and_notification() = runTest {
        val h = Harness()
        h.bridge.publish(Quotes(listOf("a")))
        h.now = 2_000L

        val result = h.bridge.publish(Quotes(listOf("a")))

        assertEquals(PublishResult.Unchanged, result)
        assertEquals(1, h.storage.writeCount)
        assertEquals(1, h.notifier.count)
    }

    @Test
    fun payload_change_produces_a_new_fingerprint_and_generation() = runTest {
        val h = Harness()
        val first = h.bridge.publish(Quotes(listOf("a"))) as PublishResult.Published
        h.now = 2_000L

        val second = h.bridge.publish(Quotes(listOf("a", "b"))) as PublishResult.Published

        assertNotEquals(first.generationId, second.generationId)
        assertEquals(2, h.storage.writeCount)
        assertEquals(2, h.notifier.count)
    }

    @Test
    fun assets_are_written_and_resolvable_by_name() = runTest {
        val h = Harness()

        val result = h.bridge.publish(Quotes(listOf("a"), image = "assets/q1.png"), assets = listOf(WidgetAsset("q1.png", ByteArray(128))))
        val feed = h.bridge.read()!!

        assertEquals(128L, (result as PublishResult.Published).assetBytes)
        assertEquals("/${result.generationId}/assets/q1.png", feed.assetPath("q1.png"))
        assertNull(feed.assetPath("missing.png"))
        assertNull(feed.assetPath("../q1.png"))
    }

    @Test
    fun asset_change_alone_is_a_new_generation() = runTest {
        val h = Harness()
        h.bridge.publish(Quotes(listOf("a")), assets = listOf(WidgetAsset("q.png", byteArrayOf(1))))

        val result = h.bridge.publish(Quotes(listOf("a")), assets = listOf(WidgetAsset("q.png", byteArrayOf(2))))

        assertIs<PublishResult.Published>(result)
    }

    @Test
    fun unsafe_asset_name_is_rejected_before_writing() = runTest {
        val h = Harness()

        assertFailsWith<IllegalArgumentException> { h.bridge.publish(Quotes(listOf("a")), assets = listOf(WidgetAsset("../evil.png", byteArrayOf(1)))) }
        assertEquals(0, h.storage.writeCount)
    }

    @Test
    fun read_returns_null_when_nothing_was_published() {
        assertNull(Harness().bridge.read())
    }

    @Test
    fun read_returns_the_typed_payload_and_metadata() = runTest {
        val h = Harness()
        h.bridge.publish(Quotes(listOf("a", "b"), emptyText = "none"))

        val feed = h.bridge.read()!!

        assertEquals(Quotes(listOf("a", "b"), emptyText = "none"), feed.payload)
        assertEquals(1_000L, feed.generatedAtEpochMs)
        assertEquals(64, feed.fingerprint.length)
    }

    @Test
    fun corrupt_current_generation_falls_back_to_the_previous_one() = runTest {
        val h = Harness()
        h.bridge.publish(Quotes(listOf("a")))
        h.storage.prependCorruptGeneration = true

        assertEquals(Quotes(listOf("a")), h.bridge.read()!!.payload)
        assertEquals(PublishResult.Unchanged, h.bridge.publish(Quotes(listOf("a"))))
    }

    @Test
    fun schema_mismatch_is_skipped_by_read_and_forces_a_rewrite() = runTest {
        val old = Harness(schemaVersion = 1)
        old.bridge.publish(Quotes(listOf("a")))
        val new = WidgetBridge(WidgetBridgeConfig(schemaVersion = 2), Quotes.serializer(), old.storage, old.notifier, clock = { 5L })

        assertNull(new.read())
        assertIs<PublishResult.Published>(new.publish(Quotes(listOf("a"))))
    }

    @Test
    fun keep_previous_is_forwarded_to_storage() = runTest {
        val h = Harness(keepPrevious = 3)
        h.bridge.publish(Quotes(listOf("a")))
        assertEquals(3, h.storage.lastKeepPrevious)
    }

    @Test
    fun envelope_on_disk_carries_schema_and_payload() = runTest {
        val h = Harness()
        h.bridge.publish(Quotes(listOf("a")))

        val envelope = FeedJson.decodeEnvelope(h.storage.generations.first().first.feedBytes)

        assertEquals(1, envelope.schemaVersion)
        assertTrue(""""quotes"""" in envelope.payload.toString())
    }

    @Test
    fun clear_removes_everything() = runTest {
        val h = Harness()
        h.bridge.publish(Quotes(listOf("a")))
        h.bridge.clear()
        assertNull(h.bridge.read())
    }
}
