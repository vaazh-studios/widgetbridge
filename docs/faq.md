# FAQ

**`WidgetFeedReader(appGroup:)` returns nil on iOS.** The App Group is missing from the widget extension target, or its identifier differs from the one the app publishes to. Both targets need the same group under Signing & Capabilities.

**`iosWidgetBridge` throws on first use.** Same cause on the app side: the App Group entitlement is not on the app target.

**`publish` says `Unchanged` but I changed something.** The fingerprint covers the payload JSON and the encoded asset bytes. If your payload has the same fields and the images encode to the same bytes, nothing is written. Timestamps are not part of the payload unless you put them there.

**Encoding 40 images on every publish is slow.** Keep a cheap fingerprint of your source data in the payload and compare it with `bridge.read()?.payload` before packing assets. See [when to publish](refresh.md).

**The Android widget shows the previous data after a second publish.** Glance keeps the composition alive for a while and only recomposes on update. Read the feed inside `provideContent`, keyed on a counter the receiver bumps. See [setup, Android](setup-android.md).

**The iOS widget did not redraw.** `WidgetBridgeReloader.start()` must run once in the app, and WidgetKit rate-limits `reloadAllTimelines` to a handful per hour. Debounce your publishes.

**Can the widget show localized strings?** Put them in the payload. The extension cannot read Compose resources. See [recipes](recipes.md).

**Can two widgets read one feed?** Yes. Both call `read()` and pick different items or layouts. See [recipes](recipes.md).

**Where are the files?** Android `<filesDir>/widgetbridge`, iOS `<App Group container>/widgetbridge`. Change the folder with `WidgetBridgeConfig.directoryName`.
