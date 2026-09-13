# Android setup

1. Add Glance to your **app** module (the library itself does not depend on it):
   ```kotlin
   implementation("androidx.glance:glance-appwidget:1.1.1")
   ```
2. Declare a receiver and a widget:
   ```kotlin
   class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
       override val glanceAppWidget = QuoteWidget()
   }
   ```
   Register it in `AndroidManifest.xml` with the `android.appwidget.action.APPWIDGET_UPDATE` intent filter and an `android.appwidget.provider` meta-data pointing at your `res/xml/<widget>_info.xml`.
3. Build the bridge with that receiver class so `publish` can broadcast the update:
   ```kotlin
   val bridge = androidWidgetBridge(context, WidgetBridgeConfig(schemaVersion = 1), QuoteFeed.serializer(), QuoteWidgetReceiver::class.java)
   ```
4. Read in `provideGlance`:
   ```kotlin
   override suspend fun provideGlance(context: Context, id: GlanceId) {
       val feed = bridge.read()
       val bitmap = feed?.assetPath("q1.jpg")?.let(BitmapFactory::decodeFile)
       provideContent { /* your Glance UI */ }
   }
   ```

**Read inside the composition.** Glance keeps a session alive for tens of seconds after a render, and `update()` on a live session only recomposes; code before `provideContent` does not run again. Bump an in-process counter from the receiver and key the read on it:
```kotlin
class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuoteWidget()
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) QuoteWidget.refreshes.value++
        super.onReceive(context, intent)
    }
}
// in provideGlance:
provideContent {
    val refresh by QuoteWidget.refreshes.collectAsState()
    val model = remember(refresh) { loadModel(bridge) }
    QuoteCard(model)
}
```

**R8 and release builds.** Glance renders through a WorkManager worker, and WorkManager creates two classes by reflection whose constructors R8 full mode strips with the keep rules WorkManager 2.7 to 2.9 and Room 2.2 to 2.6 ship: the request's `InputMerger` (the widget then stays on its loading layout forever) and `WorkDatabase_Impl` (the app crashes at launch). WidgetBridge's AAR carries consumer keep rules for both, so a minified app that depends on it needs nothing extra. If you bump `androidx.work` to 2.10 or later its own rules cover the first case; the library's rules stay harmless. Check your own release build once: the sample's `assembleRelease` is minified and CI greps its R8 usage report for both constructors.

Storage is `<filesDir>/widgetbridge` (change the folder with `WidgetBridgeConfig.directoryName`). `publish` and `read` do file I/O; call them from a background dispatcher.
