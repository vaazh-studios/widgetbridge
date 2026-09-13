# Known issues

Open items we know about. Each one has a workaround.

| Area | Issue | Workaround | Status |
|---|---|---|---|
| Glance | A live session only recomposes on update; a read done before `provideContent` goes stale | Read inside the composition, keyed on a receiver-bumped counter | Documented; sample shows it |
| WidgetKit | `reloadAllTimelines` is rate-limited by iOS | Debounce publishes; pre-schedule a timeline | By design (Apple) |
| Fingerprint | Only known after assets are encoded | Keep a source fingerprint in the payload and compare first | Documented in refresh |
| iOS | Placeholder previews in the widget gallery show your placeholder entry, not live data | Ship a good placeholder | By design (Apple) |
| R8 | WorkManager 2.7 to 2.9 keep `InputMerger` subclasses but not their constructor under R8 full mode; the Glance worker never starts and the widget shows its loading layout forever, on release builds only | The AAR ships the keep rule since 0.3.0; or use `androidx.work` 2.10+ | Fixed in 0.3.0 |
| R8 | Room 2.2 to 2.6 keep `RoomDatabase` subclasses but not their constructor; WorkManager's database fails to instantiate and the app crashes at launch, on release builds only | The AAR ships the keep rule since 0.3.0 | Fixed in 0.3.0 |

Found another? [Open an issue](https://github.com/vaazh-studios/widgetbridge/issues/new/choose).
