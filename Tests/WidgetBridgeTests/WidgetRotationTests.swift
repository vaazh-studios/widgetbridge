import XCTest
@testable import WidgetBridge

final class WidgetRotationTests: XCTestCase {
    func testHourSlotVectors() {
        XCTAssertEqual(WidgetRotation.hourSlot(epochMs: 0, zoneOffsetSeconds: 0), 0)
        XCTAssertEqual(WidgetRotation.hourSlot(epochMs: 3_600_000, zoneOffsetSeconds: 0), 1)
        XCTAssertEqual(WidgetRotation.hourSlot(epochMs: 0, zoneOffsetSeconds: 3_600), 1)
        XCTAssertEqual(WidgetRotation.hourSlot(epochMs: -1, zoneOffsetSeconds: 0), -1)
        XCTAssertEqual(WidgetRotation.hourSlot(epochMs: 1_789_250_400_000, zoneOffsetSeconds: 7_200), 497_016)
    }

    func testIndexVectors() {
        XCTAssertEqual(WidgetRotation.index(slot: 5, count: 3), 2)
        XCTAssertEqual(WidgetRotation.index(slot: 6, count: 3), 0)
        XCTAssertEqual(WidgetRotation.index(slot: -1, count: 3), 2)
    }

    func testStableHashVectors() {
        XCTAssertEqual(WidgetRotation.stableHash(""), 0x811C9DC5)
        XCTAssertEqual(WidgetRotation.stableHash("a"), 0xE40C292C)
        XCTAssertEqual(WidgetRotation.stableHash("foobar"), 0xBF9CF968)
    }

    func testDateOverloadMatchesEpochOverload() {
        let date = Date(timeIntervalSince1970: 1_789_250_400)
        let zone = TimeZone(secondsFromGMT: 7_200)!
        XCTAssertEqual(WidgetRotation.hourSlot(date, in: zone), 497_016)
    }
}
