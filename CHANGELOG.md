# Changelog

## 0.2.0 (unreleased)

- New artifact `com.vocabloot:widgetbridge-test`: `FakeWidgetFeedStorage` and `CountingNotifier`, the fakes the library's own tests run on, so an app can unit-test its publish code without a device.
- Library code unchanged from 0.1.0; the public API of `widgetbridge` is identical.
- README rewritten as a landing page; long-form guides moved to the docs site.

## 0.1.0 (2026-09-06)

- `WidgetBridge<T>`: typed publish with fingerprint dedupe, atomic generations, previous-generation fallback, schema check.
- `AndroidWidgetFeedStorage` + `AppWidgetRefreshNotifier`; `IosWidgetFeedStorage` (App Group) + `NotificationCenterRefreshNotifier`.
- `WidgetImages.encode` and `AssetBudget`; `WidgetRotation` with vectors shared with Swift.
- Swift package `WidgetBridge`: `WidgetFeedReader`, `WidgetRotation`, `WidgetBridgeReloader`.
