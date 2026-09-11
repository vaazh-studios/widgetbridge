# Known issues

Open items we know about. Each one has a workaround.

| Area | Issue | Workaround | Status |
|---|---|---|---|
| Glance | A live session only recomposes on update; a read done before `provideContent` goes stale | Read inside the composition, keyed on a receiver-bumped counter | Documented; sample shows it |
| WidgetKit | `reloadAllTimelines` is rate-limited by iOS | Debounce publishes; pre-schedule a timeline | By design (Apple) |
| Fingerprint | Only known after assets are encoded | Keep a source fingerprint in the payload and compare first | Documented in refresh |
| iOS | Placeholder previews in the widget gallery show your placeholder entry, not live data | Ship a good placeholder | By design (Apple) |

Found another? [Open an issue](https://github.com/vaazh-studios/widgetbridge/issues/new/choose).
