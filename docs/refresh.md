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

## Skip the encode when nothing changed

`publish` fingerprints the payload **and the encoded asset bytes**, so it can only answer
`Unchanged` after `AssetBudget.pack` has run. For a handful of images that is fine. For dozens
(Vocabloot ships up to 48 per generation) keep a cheap fingerprint of the source data in the
payload and compare it with the current generation first:

```kotlin
@Serializable
data class QuoteFeed(val sourceFingerprint: String, val quotes: List<Quote>, val emptyText: String)

suspend fun publishIfChanged(bridge: WidgetBridge<QuoteFeed>, quotes: List<Quote>, emptyText: String) {
    val fingerprint = quotes.joinToString("\u001f") { "${it.id}|${it.text}|${it.photoPath}" }.let(::sha256Hex)
    if (bridge.read()?.payload?.sourceFingerprint == fingerprint) return          // one file read, zero encodes
    val assets = AssetBudget().pack(quotes.map { AssetCandidate("${it.id}.jpg", it.photoPath, WidgetImageFormat.Jpeg) })
    bridge.publish(QuoteFeed(fingerprint, quotes, emptyText), assets)
}
```

The bridge still fingerprints the encoded bytes underneath, so an image that changed on disk under
the same path is caught by the second check.

The OS decides when widgets actually redraw. Android honours the update broadcast immediately for
placed widgets; iOS rate-limits `reloadAllTimelines` (roughly a handful per hour per app), so
`publish` after every keystroke is harmless for storage but pointless for the screen. Debounce.
