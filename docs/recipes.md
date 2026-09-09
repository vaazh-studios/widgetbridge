# Recipes

## Lock Screen widgets (accessory family)

The reader is the same; only the SwiftUI view differs. Declare the families you support and branch on `widgetFamily`:

```swift
struct WordWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "word", provider: WordProvider()) { entry in WordView(entry: entry) }
            .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular, .accessoryCircular])
    }
}

struct WordView: View {
    @Environment(\.widgetFamily) var family
    let entry: WordEntry
    var body: some View {
        switch family {
        case .accessoryRectangular: Text(entry.word).font(.headline)      // two lines of text, no image
        default: FullCard(entry: entry)
        }
    }
}
```

Accessory families render in monochrome and at a small size, so the payload should carry a short form of each item if the full text is long.

## Several widgets from one feed

Publish once; every widget reads the same generation and chooses its own item:

```kotlin
val feed = bridge.read() ?: return
val today = feed.payload.items[WidgetRotation.index(WidgetRotation.hourSlot(now, offsetSeconds), feed.payload.items.size)]
val favourite = feed.payload.items.firstOrNull { it.id == feed.payload.featuredId }
```

Register one Glance receiver per widget on Android and pass any of them as `receiverClass`; the broadcast reaches every placed widget of that receiver, so use the one users are most likely to have placed, or call `AppWidgetRefreshNotifier` for each.

## Localized payloads

The extension has no access to your string resources. Resolve every string in the app, at publish time, in the app's current locale:

```kotlin
@Serializable
data class Feed(val items: List<Item>, val emptyTitle: String, val emptyBody: String, val locale: String)

bridge.publish(Feed(items, emptyTitle = strings.widgetEmptyTitle(), emptyBody = strings.widgetEmptyBody(), locale = currentLocaleTag()))
```

Republish when the locale changes; the fingerprint sees the new strings and writes a new generation.

## A word-of-the-day timeline on iOS

WidgetKit lets you pre-schedule entries, so the widget can flip on the hour without the app running:

```swift
func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
    guard let feed = reader?.read(Feed.self) else { return completion(Timeline(entries: [placeholder], policy: .atEnd)) }
    let hours = (0..<24).map { Calendar.current.date(byAdding: .hour, value: $0, to: Date().startOfHour)! }
    let entries = hours.map { date in
        let index = WidgetRotation.index(slot: WidgetRotation.hourSlot(date), count: feed.payload.items.count)
        return Entry(date: date, item: feed.payload.items[index], image: feed.assetURL(feed.payload.items[index].image))
    }
    completion(Timeline(entries: entries, policy: .atEnd))
}
```

Android has no timeline; Glance redraws on `updatePeriodMillis` and on your publish broadcast, and `WidgetRotation` gives the same item for the same hour on both platforms.
