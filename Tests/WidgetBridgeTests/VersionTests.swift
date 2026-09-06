import XCTest
@testable import WidgetBridge

final class VersionTests: XCTestCase {
    func testVersion() { XCTAssertEqual(WidgetBridgeVersion.current, "0.1.0") }
}
