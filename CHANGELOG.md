# Changelog

## 0.3.0 (2026-09-13)

- The AAR ships consumer keep rules for the two classes WorkManager creates by reflection, `InputMerger` subclasses and `WorkDatabase_Impl`. R8 full mode dropped their constructors with the rules WorkManager 2.7 to 2.9 and Room 2.2 to 2.6 ship, so a minified app's Glance widget stayed on its loading layout forever, or the app crashed at launch. Found in Vocabloot's Play release; no app change needed once on 0.3.0.
- The sample's release build is minified, and CI checks R8's usage report to prove the rules reach a consumer.
- Public API unchanged.

## 0.2.0 (2026-09-09)

- New artifact `com.vocabloot:widgetbridge-test`: `FakeWidgetFeedStorage` and `CountingNotifier`, the fakes the library's own tests run on, so an app can unit-test its publish code without a device.
- Library code unchanged from 0.1.0; the public API of `widgetbridge` is identical.
- README rewritten as a landing page; long-form guides moved to the docs site.

## 0.1.0 (2026-09-06)

- `WidgetBridge<T>`: typed publish with fingerprint dedupe, atomic generations, previous-generation fallback, schema check.
- `AndroidWidgetFeedStorage` + `AppWidgetRefreshNotifier`; `IosWidgetFeedStorage` (App Group) + `NotificationCenterRefreshNotifier`.
- `WidgetImages.encode` and `AssetBudget`; `WidgetRotation` with vectors shared with Swift.
- Swift package `WidgetBridge`: `WidgetFeedReader`, `WidgetRotation`, `WidgetBridgeReloader`.
