# Changelog

## 0.1.0 (2026-09-06)

- `WidgetBridge<T>`: typed publish with fingerprint dedupe, atomic generations, previous-generation fallback, schema check.
- `AndroidWidgetFeedStorage` + `AppWidgetRefreshNotifier`; `IosWidgetFeedStorage` (App Group) + `NotificationCenterRefreshNotifier`.
- `WidgetImages.encode` and `AssetBudget`; `WidgetRotation` with vectors shared with Swift.
- Swift package `WidgetBridge`: `WidgetFeedReader`, `WidgetRotation`, `WidgetBridgeReloader`.
