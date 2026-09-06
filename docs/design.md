# WidgetBridge: a Kotlin Multiplatform library for handing data to home-screen widgets

**Date:** 2026-09-06
**Status:** approved design (founder, 2026-09-06)
**Repo:** `vaazh-studios/widgetbridge` (public, Apache 2.0), sibling checkout at `../widgetbridge`
**Artifacts:** Maven Central `com.vocabloot:widgetbridge` (Kotlin), Swift package `WidgetBridge` (same repo, `Package.swift` at root)
**Origin:** Vocabloot's shipped widget feed (`shared/data/.../source/widget/`, `androidApp/.../widget/AndroidWidgetFeedReader.kt`, `iosApp/SnavoWidget/WidgetFeedStore.swift`), made app-agnostic. Vocabloot itself is not changed by this work.

## 1. The problem

Home-screen and lock-screen widgets cannot run the app's shared Kotlin:

- On iOS the widget is a separate extension process with a memory ceiling around 30 MB. Linking the Kotlin framework into it gets the extension killed (Kotlin Slack, #multiplatform: `EXC_RESOURCE ... limit=30 MB`) and can fail App Store validation ("disallowed nested bundles", Kotlin Discussions #28406). WidgetKit itself is Swift-only.
- On Android, Glance widgets wake when the app is not running and must not depend on its initialised state.

So every KMP app with a widget hand-rolls the same handoff: the app writes what the widget should show, the widget reads it. Done naively (one JSON string in UserDefaults, which is what MatchPin, WARP and every iOS blog do) it has no atomicity across files, no images, no recovery from a corrupt write, and no schema versioning. Nothing on klibs.io or the Swift Package Index packages this.

## 2. What it is

One Kotlin entry point, `WidgetBridge<T>`, that publishes a typed payload plus image assets into a **generation folder** and switches an atomic pointer; a Kotlin reader for Glance; a Swift package reader for WidgetKit; helpers for image encoding under a byte budget, deterministic rotation, and "tell the OS to redraw". No UI, no scheduling.

Patterns adopted (from the BackupKit research): filesystem-style verbs and a typed payload (react-native-cloud-storage, KStore), ten-line quickstart (IceCream), explicit setup pages per platform (icloud_storage), typed outcomes instead of exceptions for expected states.

## 3. Goals and non-goals

Goals

1. `implementation("com.vocabloot:widgetbridge:0.1.0")` plus the Swift package, then a working widget feed in about ten lines of Kotlin and ten of Swift.
2. Identical read semantics in Kotlin and Swift: same candidate order, same fallback, same safety rules, same rotation math.
3. The widget extension never links Kotlin.
4. Crash at any point during a publish leaves either the old or the new generation readable, never a mix.

Non-goals: widget UI (WARP covers that), background refresh scheduling (README shows a Flow-driven trigger), Live Activities, lock-screen specifics, Compose resources access from the extension (payloads carry pre-localised strings).

## 4. Repository, module, toolchain

```
widgetbridge/
├── widgetbridge/                # Kotlin module, published
│   └── src/{commonMain,androidMain,iosMain,commonTest,androidHostTest,iosSimulatorArm64Test}
├── Package.swift                # Swift package "WidgetBridge" (library target Sources/WidgetBridge)
├── Sources/WidgetBridge/        # WidgetFeedReader.swift, WidgetRotation.swift, WidgetBridgeReloader.swift
├── Tests/WidgetBridgeTests/     # XCTest for the Swift reader against fixture folders
├── sample/                      # "Quote of the day": shared (KMP+Compose), androidApp (Glance widget), iosApp (xcodegen, WidgetKit extension)
├── docs/                        # setup-android.md, setup-ios.md, feed-format.md, refresh.md, publishing.md
├── .github/workflows/           # ci.yml, publish.yml (as BackupKit)
└── README.md, CHANGELOG.md, LICENSE, api/
```

- Targets: `android` (compileSdk 36, minSdk 24, JVM 21, `withHostTest {}`), `iosArm64`, `iosSimulatorArm64`, `iosX64`. Explicit API mode. BCV klib ABI dump.
- Toolchain as BackupKit: Kotlin 2.3.20, AGP 9.2.1, Gradle 9.4.1, vanniktech 0.37.0.
- Dependencies: `kotlinx-coroutines-core` (api), `kotlinx-serialization-json` (api: the payload is `@Serializable`). Android: nothing else (java.io, `AppWidgetManager`). iOS: nothing else (Foundation). No Glance dependency in the library; the sample uses Glance.
- Swift package: iOS 16+, no dependencies, Foundation + WidgetKit (WidgetKit only in `WidgetBridgeReloader`, which the app target uses).

## 5. Kotlin public API (package `com.vocabloot.widgetbridge`)

### 5.1 Configuration and construction

```kotlin
public data class WidgetBridgeConfig(
    /** Bumped by the app when the payload shape changes incompatibly; readers refuse other versions. */
    val schemaVersion: Int,
    /** Folder under the platform root. Android: `<filesDir>/<name>`; iOS: `<App Group container>/<name>`. */
    val directoryName: String = "widgetbridge",
    /** iOS only, required there: `group.com.example.app`. Ignored on Android. */
    val iosAppGroup: String? = null,
    /** Generations kept besides the current one, for fallback. */
    val keepPrevious: Int = 1,
)

public class WidgetBridge<T>(
    config: WidgetBridgeConfig,
    serializer: KSerializer<T>,
    storage: WidgetFeedStorage,          // platform default via widgetFeedStorage(config, context) / widgetFeedStorage(config)
    notifier: WidgetRefreshNotifier,     // platform default, see 5.4; WidgetRefreshNotifier.None for tests
)
```

Platform factories (expect/actual style, but plain functions):

- Android: `fun androidWidgetBridge(context, config, serializer, receiverClass: Class<out BroadcastReceiver>): WidgetBridge<T>`
- iOS: `fun iosWidgetBridge(config, serializer): WidgetBridge<T>` (requires `config.iosAppGroup`)

### 5.2 Publish

```kotlin
public class WidgetAsset(public val fileName: String, public val bytes: ByteArray)   // safe name: letters, digits, - _ .

public sealed interface PublishResult {
    /** Content fingerprint matched the current generation; nothing written, no redraw requested. */
    public data object Unchanged : PublishResult
    public data class Published(val generationId: String, val assetBytes: Long) : PublishResult
}

public suspend fun publish(payload: T, assets: List<WidgetAsset> = emptyList()): PublishResult
```

Behaviour: serialise the payload with the app's serializer (deterministic: `encodeDefaults = true`, `explicitNulls = true`); fingerprint = sha256 over schema version, payload JSON, and each asset's name and bytes; if equal to the current generation's fingerprint return `Unchanged`; else write `generations/<id>.tmp/` (assets under `assets/`, then `feed.json`), rename to `generations/<id>/`, write the pointer atomically (`current.json` = `{current, previous...}`), delete generations not referenced, call `notifier.notifyChanged()`. `<id>` = `<generatedAtEpochMs>-<fingerprint8>`. Serialised through a mutex so concurrent publishes never interleave.

### 5.3 Read (Kotlin side, for Glance and for the app's own checks)

```kotlin
public class ResolvedFeed<T>(
    public val payload: T,
    public val generationId: String,
    public val generatedAtEpochMs: Long,
    public val fingerprint: String,
    public val directory: String,
) {
    /** Absolute path of an asset, or null when the name escapes the generation folder or the file is missing. */
    public fun assetPath(fileName: String): String?
}

public fun read(): ResolvedFeed<T>?
public fun clear()
```

Candidate order: pointer.current, pointer.previous, then every non-temp generation directory newest first; the first that parses and matches `schemaVersion` wins. Corrupt or foreign files never throw out of `read()`.

### 5.4 Refresh notifier

```kotlin
public fun interface WidgetRefreshNotifier {
    public fun notifyChanged()
    public companion object { public val None: WidgetRefreshNotifier }
}
```

- Android `AppWidgetRefreshNotifier(context, receiverClass)`: looks up the widget ids for the receiver and broadcasts `ACTION_APPWIDGET_UPDATE` with them. No Glance import; Glance's receiver handles the broadcast.
- iOS `NotificationCenterRefreshNotifier()`: posts `WidgetBridgeFeedChanged` on `NSNotificationCenter`. The app's Swift side starts `WidgetBridgeReloader` once (5.7), which calls `WidgetCenter.shared.reloadAllTimelines()`.

### 5.5 Images

```kotlin
public enum class WidgetImageFormat { Png, Jpeg }
public class EncodedImage(public val bytes: ByteArray, public val extension: String)

public object WidgetImages {
    /** Downsample [sourcePath] so its long side is at most [maxDimension] and the encoding fits [maxBytes]; null when unreadable. */
    public fun encode(sourcePath: String, format: WidgetImageFormat, maxDimension: Int = 512, maxBytes: Int = 2 * 1024 * 1024): EncodedImage?
}

/** Greedy packer: encodes in priority order until [budgetBytes] is spent. */
public class AssetBudget(public val budgetBytes: Long = 8L * 1024 * 1024) {
    public fun pack(candidates: List<AssetCandidate>): List<WidgetAsset>
}
public class AssetCandidate(public val fileName: String, public val sourcePath: String, public val format: WidgetImageFormat)
```

Android: `BitmapFactory` with `inSampleSize`, EXIF orientation honoured, `Bitmap.compress`. iOS: `UIImage` scaled with `UIGraphicsImageRenderer`, `UIImagePNGRepresentation` / `UIImageJPEGRepresentation(0.8)`. Both ported from Vocabloot's encoders.

### 5.6 Rotation

```kotlin
public object WidgetRotation {
    public fun hourSlot(epochMs: Long, zoneOffsetSeconds: Int): Long
    public fun index(slot: Long, count: Int): Int
    public fun stableHash(value: String): UInt   // FNV-1a 32
}
```

Swift twin with identical results; a shared test vector file (`docs/rotation-vectors.json`) is checked by both test suites.

### 5.7 Swift package `WidgetBridge`

```swift
public struct ResolvedFeed<Payload: Decodable> {
    public let payload: Payload
    public let generationId: String
    public let generatedAtEpochMs: Int64
    public let directory: URL
    public func assetURL(_ fileName: String) -> URL?
}

public struct WidgetFeedReader {
    public init(appGroup: String, directoryName: String = "widgetbridge", schemaVersion: Int)
    public func read<Payload: Decodable>(_ type: Payload.Type) -> ResolvedFeed<Payload>?
}

public enum WidgetRotation { static func hourSlot(_ date: Date) -> Int64; static func index(slot: Int64, count: Int) -> Int }

/// App target only: start once at launch; observes WidgetBridgeFeedChanged and reloads all timelines.
public enum WidgetBridgeReloader { public static func start() }
```

Same candidate order, schema check, temp-suffix skip, safe-name and path-prefix checks as Kotlin.

### 5.8 Ten-line quickstart

```kotlin
@Serializable data class QuoteFeed(val quotes: List<Quote>, val emptyText: String)

val bridge = androidWidgetBridge(context, WidgetBridgeConfig(schemaVersion = 1), QuoteFeed.serializer(), QuoteWidgetReceiver::class.java)
bridge.publish(QuoteFeed(quotes, emptyText = getString(R.string.widget_empty)), assets = AssetBudget().pack(candidates))

// Glance
val feed = bridge.read() ?: return provideContent { EmptyCard() }
val quote = feed.payload.quotes[WidgetRotation.index(WidgetRotation.hourSlot(now, offset), feed.payload.quotes.size)]
```

```swift
let feed = WidgetFeedReader(appGroup: "group.com.example.quotes", schemaVersion: 1).read(QuoteFeed.self)
```

## 6. On-disk format (documented in `docs/feed-format.md`, stable within a major version)

```
<root>/<directoryName>/
  current.json            {"current":"<id>","previous":["<id>"]}
  generations/<id>/
    feed.json             {"schemaVersion":1,"generatedAtEpochMs":...,"fingerprint":"<sha256 hex>","payload":{...}}
    assets/<fileName>
  generations/<id>.tmp/   in-progress, ignored by readers
```

## 7. Tests

- `commonTest`: `WidgetBridgeTest` against `FakeWidgetFeedStorage` (ported from Vocabloot's 14 repository cases): identical content skips write and notify; assets linked by relative path; schema mismatch falls back; corrupt current falls back to previous; new fingerprint on payload change; budget cut-off; safe-name rejection; pointer prune keeps `keepPrevious`. `WidgetRotationTest` against `docs/rotation-vectors.json`.
- `androidHostTest`: `AndroidWidgetFeedStorageTest` on a temp dir (real files, rename, prune, temp-suffix skip).
- `iosSimulatorArm64Test`: `IosWidgetFeedStorageTest` using the internal root-path override (the simulator has no App Group entitlement).
- Swift: `WidgetFeedReaderTests` against fixture folders (current ok, current corrupt, schema mismatch, unsafe asset path) and rotation vectors.
- CI (macOS): Gradle build + host + simulator tests + `apiCheck`, `swift test`, sample Android build.

## 8. Sample

"Quote of the day": a list of quotes with optional images, one Glance widget (small/medium) and one WidgetKit widget (small/medium), hourly rotation, a "Feature this quote for an hour" action showing a payload-level override, a Flow-driven republish when the list changes. Own App Group `group.com.vocabloot.widgetbridge.sample`.

## 9. Docs and publishing

README in the BackupKit pattern: badges, Features, support matrix, Who's using it (Vocabloot, store links), Install (Gradle + SPM), Quickstart, setup pages, feed format, refresh, limits, comparison (MatchPin, WARP, Fidget, hand-rolled UserDefaults), Dependencies, License. Publishing via the BackupKit pipeline (namespace `com.vocabloot` already verified; new repo secrets from Doppler project `backupkit`, or a `widgetbridge` Doppler project with the same values). Swift package consumers use the GitHub URL and a version tag; the same `v0.1.0` tag serves both.

## 10. Founder tasks

1. GitHub secrets for the new repo (same five as BackupKit; the assistant can copy the three signing values, the Maven token pair is founder-only).
2. Apple portal: App Group `group.com.vocabloot.widgetbridge.sample` for the sample.
3. Press Publish on the Central Portal for the release.

## 11. Sequence

1. Scaffold repo, Gradle, Swift package skeleton, CI (day 1).
2. Port storage (Android, iOS), envelope, `WidgetBridge`, notifier, tests (day 1 to 2).
3. Images, rotation, vectors (day 2).
4. Swift reader + tests, sample, docs (day 3).
5. Public repo, release `v0.1.0`, Maven Central publish.
