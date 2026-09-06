# Module WidgetBridge

Typed, atomic handoff of data and images from a Kotlin Multiplatform app to its Glance and WidgetKit widgets.

# Package com.vocabloot.widgetbridge

`WidgetBridge` publishes a `@Serializable` payload plus `WidgetAsset`s as an atomic generation and
reads it back as a `ResolvedFeed`. Platform wiring: `androidWidgetBridge` / `iosWidgetBridge`.
Helpers: `WidgetImages` and `AssetBudget` for images, `WidgetRotation` for hourly rotation shared
with the Swift package. The Swift reader lives in `Sources/WidgetBridge`.
