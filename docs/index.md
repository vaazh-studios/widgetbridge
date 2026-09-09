# WidgetBridge

Typed, atomic handoff of data and images from a Kotlin Multiplatform app to its home-screen widgets: Jetpack Glance on Android, WidgetKit on iOS. **The widget extension never links Kotlin.**

![WidgetBridge](assets/hero.png)

## Start here

1. [Setup, Android](setup-android.md): a Glance receiver and the read-inside-the-composition rule.
2. [Setup, iOS](setup-ios.md): the App Group on both targets and the Swift package.
3. The quickstart on the [README](https://github.com/vaazh-studios/widgetbridge#widgetbridge-101), then [when to publish](refresh.md).

## Guides

- [How it works](how-it-works.md): generations, the pointer, the redraw request.
- [Feed format and reader rules](feed-format.md): the on-disk contract both readers implement.
- [When to publish](refresh.md): debounce, foreground, and the pre-encode fingerprint pattern.
- [Recipes](recipes.md): Lock Screen family, several widgets from one feed, localized payloads, a word-of-the-day timeline.
- [FAQ](faq.md) and [known issues](known-issues.md).
- [Stability and versions](stability.md).

## Reference

- [API reference](api/index.html) (Dokka), binary API tracked in `widgetbridge/api`.
- [Design](design.md) and [publishing](publishing.md), for maintainers.
