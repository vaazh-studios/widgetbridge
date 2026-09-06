package com.vocabloot.widgetbridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WidgetRotationTest {
    @Test
    fun hour_slot_vectors() {
        assertEquals(0L, WidgetRotation.hourSlot(epochMs = 0L, zoneOffsetSeconds = 0))
        assertEquals(1L, WidgetRotation.hourSlot(epochMs = 3_600_000L, zoneOffsetSeconds = 0))
        assertEquals(1L, WidgetRotation.hourSlot(epochMs = 0L, zoneOffsetSeconds = 3_600))
        assertEquals(-1L, WidgetRotation.hourSlot(epochMs = -1L, zoneOffsetSeconds = 0))
        assertEquals(497_016L, WidgetRotation.hourSlot(epochMs = 1_789_250_400_000L, zoneOffsetSeconds = 7_200))
    }

    @Test
    fun index_vectors_and_negative_slots() {
        assertEquals(2, WidgetRotation.index(slot = 5L, count = 3))
        assertEquals(0, WidgetRotation.index(slot = 6L, count = 3))
        assertEquals(2, WidgetRotation.index(slot = -1L, count = 3))
        assertFailsWith<IllegalArgumentException> { WidgetRotation.index(slot = 1L, count = 0) }
    }

    @Test
    fun stable_hash_vectors() {
        assertEquals(0x811C9DC5u, WidgetRotation.stableHash(""))
        assertEquals(0xE40C292Cu, WidgetRotation.stableHash("a"))
        assertEquals(0xBF9CF968u, WidgetRotation.stableHash("foobar"))
    }
}
