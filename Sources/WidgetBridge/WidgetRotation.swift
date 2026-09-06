import Foundation

/// Deterministic hourly rotation, identical to the Kotlin `WidgetRotation`, so both platforms show the same item.
public enum WidgetRotation {
    private static let hourMs: Int64 = 3_600_000

    /// Local wall-clock hour slot; `zoneOffsetSeconds` is the zone offset at `epochMs`.
    public static func hourSlot(epochMs: Int64, zoneOffsetSeconds: Int) -> Int64 {
        floorDiv(epochMs + Int64(zoneOffsetSeconds) * 1_000, hourMs)
    }

    /// Convenience over `hourSlot(epochMs:zoneOffsetSeconds:)` for a `Date` in `zone`.
    public static func hourSlot(_ date: Date, in zone: TimeZone = .current) -> Int64 {
        hourSlot(epochMs: Int64((date.timeIntervalSince1970 * 1_000).rounded(.down)), zoneOffsetSeconds: zone.secondsFromGMT(for: date))
    }

    /// Index into a list of `count` items for `slot`; wraps and handles negative slots.
    public static func index(slot: Int64, count: Int) -> Int {
        precondition(count > 0, "count must be positive")
        let remainder = slot % Int64(count)
        return Int(remainder < 0 ? remainder + Int64(count) : remainder)
    }

    /// FNV-1a 32-bit over UTF-8 bytes, for stable per-item salts.
    public static func stableHash(_ value: String) -> UInt32 {
        var hash: UInt32 = 0x811C9DC5
        for byte in value.utf8 {
            hash = (hash ^ UInt32(byte)) &* 0x01000193
        }
        return hash
    }

    private static func floorDiv(_ dividend: Int64, _ divisor: Int64) -> Int64 {
        let quotient = dividend / divisor
        let remainder = dividend % divisor
        return (remainder != 0 && (remainder < 0) != (divisor < 0)) ? quotient - 1 : quotient
    }
}
