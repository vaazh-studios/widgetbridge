# When to publish

`publish` is one call, safe to repeat: unchanged content is detected by fingerprint and costs one
directory listing. A typical app republishes when its data or locale changes, with a short debounce,
and once on foreground in case a publish was interrupted:

```kotlin
class WidgetPublisher(private val bridge: WidgetBridge<QuoteFeed>, private val quotes: Flow<List<Quote>>, private val locale: Flow<String>, scope: CoroutineScope) {
    init {
        scope.launch(Dispatchers.IO) {
            combine(quotes, locale) { q, l -> buildFeed(q, l) }
                .debounce(500.milliseconds)
                .collectLatest { feed -> bridge.publish(feed) }
        }
    }
}
```

The OS decides when widgets actually redraw. Android honours the update broadcast immediately for
placed widgets; iOS rate-limits `reloadAllTimelines` (roughly a handful per hour per app), so
`publish` after every keystroke is harmless for storage but pointless for the screen. Debounce.
