import Foundation

/// The generation a reader resolved: current, or the newest readable fallback.
public struct ResolvedFeed<Payload: Decodable> {
    public let payload: Payload
    public let generationId: String
    public let generatedAtEpochMs: Int64
    public let fingerprint: String
    public let directory: URL

    /// Absolute URL of `assets/<fileName>` inside this generation, or nil when unsafe or missing.
    public func assetURL(_ fileName: String) -> URL? {
        guard WidgetFeedReader.isSafeName(fileName) else { return nil }
        let base = directory.standardizedFileURL
        let candidate = base.appendingPathComponent("assets", isDirectory: true).appendingPathComponent(fileName).standardizedFileURL
        guard candidate.path.hasPrefix(base.path + "/"), FileManager.default.fileExists(atPath: candidate.path) else { return nil }
        return candidate
    }
}

/// Reads what the Kotlin `WidgetBridge` published. Use it from a WidgetKit extension; it never touches Kotlin.
public struct WidgetFeedReader {
    private let rootDirectory: URL
    private let schemaVersion: Int
    private let fileManager = FileManager.default
    private let decoder = JSONDecoder()

    /// `<App Group container>/<directoryName>`; must match `WidgetBridgeConfig` on the Kotlin side.
    public init?(appGroup: String, directoryName: String = "widgetbridge", schemaVersion: Int) {
        guard let container = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup) else { return nil }
        self.init(rootDirectory: container.appendingPathComponent(directoryName, isDirectory: true), schemaVersion: schemaVersion)
    }

    /// An explicit root, for tests or custom locations.
    public init(rootDirectory: URL, schemaVersion: Int) {
        self.rootDirectory = rootDirectory
        self.schemaVersion = schemaVersion
    }

    private struct Pointer: Decodable { let current: String; let previous: [String]? }
    private struct Envelope<P: Decodable>: Decodable { let schemaVersion: Int; let generatedAtEpochMs: Int64; let fingerprint: String; let payload: P }

    public func read<Payload: Decodable>(_ type: Payload.Type) -> ResolvedFeed<Payload>? {
        let generations = rootDirectory.appendingPathComponent("generations", isDirectory: true)
        let pointer = (try? Data(contentsOf: rootDirectory.appendingPathComponent("current.json"))).flatMap { try? decoder.decode(Pointer.self, from: $0) }
        let names = ((try? fileManager.contentsOfDirectory(at: generations, includingPropertiesForKeys: [.isDirectoryKey], options: [.skipsHiddenFiles])) ?? [])
            .filter { (try? $0.resourceValues(forKeys: [.isDirectoryKey]).isDirectory) == true && !$0.lastPathComponent.hasSuffix(".tmp") }
            .map(\.lastPathComponent)
            .sorted(by: >)
        var candidates: [String] = []
        if let current = pointer?.current { candidates.append(current) }
        candidates.append(contentsOf: pointer?.previous ?? [])
        candidates.append(contentsOf: names)
        var seen = Set<String>()
        for id in candidates where seen.insert(id).inserted && Self.isSafeName(id) {
            let directory = generations.appendingPathComponent(id, isDirectory: true)
            guard let data = try? Data(contentsOf: directory.appendingPathComponent("feed.json")),
                  let envelope = try? decoder.decode(Envelope<Payload>.self, from: data),
                  envelope.schemaVersion == schemaVersion else { continue }
            return ResolvedFeed(payload: envelope.payload, generationId: id, generatedAtEpochMs: envelope.generatedAtEpochMs, fingerprint: envelope.fingerprint, directory: directory)
        }
        return nil
    }

    static func isSafeName(_ value: String) -> Bool {
        !value.isEmpty && value.allSatisfy { $0.isLetter || $0.isNumber || $0 == "-" || $0 == "_" || $0 == "." }
    }
}
