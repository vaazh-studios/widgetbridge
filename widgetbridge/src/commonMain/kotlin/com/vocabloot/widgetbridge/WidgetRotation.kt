package com.vocabloot.widgetbridge

/** Deterministic hourly rotation shared by Kotlin and Swift readers so both platforms show the same item. */
public object WidgetRotation {
    private const val HOUR_MS = 3_600_000L

    /** Local wall-clock hour slot; [zoneOffsetSeconds] is the zone offset at [epochMs]. */
    public fun hourSlot(epochMs: Long, zoneOffsetSeconds: Int): Long = floorDiv(epochMs + zoneOffsetSeconds * 1_000L, HOUR_MS)

    /** Index into a list of [count] items for [slot]; wraps and handles negative slots. */
    public fun index(slot: Long, count: Int): Int {
        require(count > 0) { "count must be positive" }
        val remainder = slot % count.toLong()
        return (if (remainder < 0L) remainder + count else remainder).toInt()
    }

    /** FNV-1a 32-bit over UTF-8 bytes, for stable per-item salts. */
    public fun stableHash(value: String): UInt {
        var hash = 0x811C9DC5u
        value.encodeToByteArray().forEach { byte -> hash = (hash xor byte.toUByte().toUInt()) * 0x01000193u }
        return hash
    }

    private fun floorDiv(dividend: Long, divisor: Long): Long {
        val quotient = dividend / divisor
        val remainder = dividend % divisor
        return if (remainder != 0L && (remainder < 0L) != (divisor < 0L)) quotient - 1L else quotient
    }
}
