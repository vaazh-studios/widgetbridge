# Contributing

Thanks for looking. WidgetBridge is small on purpose; the best contributions keep it that way.

**Bugs.** Open an issue with the platform, what `read()` (Kotlin) or `WidgetFeedReader.read` (Swift) returned, and the smallest snippet that reproduces it. The on-disk layout under `generations/` is often the fastest clue.

**Changes.** Open an issue first for anything that touches the public API or the feed format (`docs/feed-format.md`): the Kotlin writer, the Kotlin reader and the Swift reader must stay in lockstep, and both test suites check the shared rotation vectors in `docs/rotation-vectors.json`. For fixes, a pull request with a test is enough:

```bash
./gradlew :widgetbridge:testAndroidHostTest :widgetbridge:iosSimulatorArm64Test :widgetbridge:apiCheck
swift test
```

`apiCheck` fails when the public API changed; run `./gradlew :widgetbridge:apiDump` and commit the diff when the change is intended.

**Style.** Explicit API mode, KDoc on every public declaration, no new dependencies without an issue explaining why. The widget extension must keep working without linking Kotlin.
