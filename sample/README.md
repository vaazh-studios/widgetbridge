# WidgetBridge sample

A quote-of-the-day app in Compose Multiplatform with a Glance widget and a WidgetKit widget over one feed. It is the README quickstart with a UI around it.

| What it shows | Where | Recording |
|---|---|---|
| Publish with a debounce whenever the data changes | `shared/.../App.kt` | ![Android](../docs/assets/demo-android.gif) |
| Images packed under the asset budget | `shared/.../QuoteStore.kt`, `SampleImages.kt` | ![iOS](../docs/assets/demo-ios.gif) |
| Glance widget reading inside the composition, keyed on the receiver's counter | `androidApp/.../QuoteWidget.kt`, `QuoteWidgetReceiver.kt` | |
| WidgetKit widget with a 24-hour pre-scheduled timeline, no Kotlin linked | `iosApp/QuoteWidget/QuoteWidget.swift` | |
| "Feature this quote" override and the hourly slideshow via `WidgetRotation` | `App.kt`, both widgets | |

MP4 versions: [Android](../docs/assets/demo-android.mp4), [iOS](../docs/assets/demo-ios.mp4). Recorded on a Pixel 9 Pro XL emulator and an iPhone 17 Pro simulator.

## Run it

**Android.** `./gradlew :sample:androidApp:installDebug`, open the app, press "Add widget to home screen" (or long-press the launcher and pick WidgetBridge sample). Add a quote; the widget updates within a second.

**iOS.** Open `sample/iosApp` in Xcode, set your team, keep the App Group `group.com.vocabloot.widgetbridge.sample` on both targets (or change it in `Platform.ios.kt`, `QuoteWidget.swift` and both entitlements files). Run the app once, then add the widget from the home-screen gallery.

The sample has no backend and no accounts.
