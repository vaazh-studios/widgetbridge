# iOS setup

1. **App Group.** Apple Developer portal → Identifiers → App Groups → create `group.com.example.app`. In Xcode add the **App Groups** capability with that group to **both** the app target and the widget extension target.
2. **Swift package.** Add `https://github.com/vaazh-studios/widgetbridge` to both targets (the app needs it for the reloader, the extension for the reader).
3. **Kotlin side** (shared code): 
   ```kotlin
   val bridge = iosWidgetBridge(WidgetBridgeConfig(schemaVersion = 1, iosAppGroup = "group.com.example.app"), QuoteFeed.serializer())
   ```
4. **App target**, once at launch (`AppDelegate` or your `@main` `init`):
   ```swift
   WidgetBridgeReloader.start()
   ```
   It observes the notification the Kotlin side posts after a publish and calls `WidgetCenter.shared.reloadAllTimelines()`. WidgetKit is Swift-only, so this hop is unavoidable.
5. **Widget extension**, in the `TimelineProvider`:
   ```swift
   let reader = WidgetFeedReader(appGroup: "group.com.example.app", schemaVersion: 1)
   let feed = reader?.read(QuoteFeed.self)
   let image = feed?.assetURL("q1.jpg").flatMap { UIImage(contentsOfFile: $0.path) }
   ```
   Your `QuoteFeed` is a plain `Decodable` mirroring the Kotlin `@Serializable` class.

The extension never imports the Kotlin framework. Storage is `<App Group container>/widgetbridge`. Simulator note: App Group containers work on the simulator without a paid team, but the entitlement must be present on both targets.
