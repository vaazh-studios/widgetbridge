import XCTest
@testable import WidgetBridge

final class WidgetFeedReaderTests: XCTestCase {
    struct Quotes: Decodable, Equatable { let quotes: [String]; let emptyText: String }

    private var root: URL!

    override func setUp() {
        root = FileManager.default.temporaryDirectory.appendingPathComponent("widgetbridge-\(UUID().uuidString)", isDirectory: true)
        try! FileManager.default.createDirectory(at: root.appendingPathComponent("generations"), withIntermediateDirectories: true)
    }

    private func writeGeneration(_ id: String, schema: Int = 1, payload: String = #"{"quotes":["a"],"emptyText":"none"}"#, asset: (String, Data)? = nil) {
        let dir = root.appendingPathComponent("generations/\(id)", isDirectory: true)
        try! FileManager.default.createDirectory(at: dir.appendingPathComponent("assets"), withIntermediateDirectories: true)
        let feed = #"{"schemaVersion":\#(schema),"generatedAtEpochMs":1000,"fingerprint":"f","payload":\#(payload)}"#
        try! feed.data(using: .utf8)!.write(to: dir.appendingPathComponent("feed.json"))
        if let (name, data) = asset { try! data.write(to: dir.appendingPathComponent("assets/\(name)")) }
    }

    private func writePointer(current: String, previous: [String] = []) {
        let json = try! JSONSerialization.data(withJSONObject: ["current": current, "previous": previous])
        try! json.write(to: root.appendingPathComponent("current.json"))
    }

    private func reader(schema: Int = 1) -> WidgetFeedReader { WidgetFeedReader(rootDirectory: root, schemaVersion: schema) }

    func testReadsCurrentGenerationAndResolvesAsset() {
        writeGeneration("g1", asset: ("q.png", Data([1, 2, 3])))
        writePointer(current: "g1")

        let feed = reader().read(Quotes.self)!

        XCTAssertEqual(feed.payload, Quotes(quotes: ["a"], emptyText: "none"))
        XCTAssertEqual(feed.generationId, "g1")
        XCTAssertNotNil(feed.assetURL("q.png"))
        XCTAssertNil(feed.assetURL("../feed.json"))
        XCTAssertNil(feed.assetURL("missing.png"))
    }

    func testCorruptCurrentFallsBackToPrevious() {
        writeGeneration("g1")
        writeGeneration("g2", payload: "{not json")
        writePointer(current: "g2", previous: ["g1"])

        XCTAssertEqual(reader().read(Quotes.self)?.generationId, "g1")
    }

    func testSchemaMismatchIsSkipped() {
        writeGeneration("g1", schema: 2)
        writePointer(current: "g1")

        XCTAssertNil(reader(schema: 1).read(Quotes.self))
    }

    func testMissingPointerFallsBackToNewestDirectoryAndSkipsTemp() {
        writeGeneration("g1")
        writeGeneration("g2")
        try! FileManager.default.createDirectory(at: root.appendingPathComponent("generations/g9.tmp"), withIntermediateDirectories: true)

        XCTAssertEqual(reader().read(Quotes.self)?.generationId, "g2")
    }

    func testEmptyRootReadsNil() {
        XCTAssertNil(reader().read(Quotes.self))
    }
}
