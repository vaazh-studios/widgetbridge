import SwiftUI
import WidgetKit
import WidgetBridge

struct Quote: Decodable { let id: String; let text: String; let author: String; let image: String? }
struct QuoteFeed: Decodable { let quotes: [Quote]; let emptyTitle: String; let emptyBody: String; let featuredId: String?; let featuredUntilEpochMs: Int64? }

struct Entry: TimelineEntry { let date: Date; let quote: Quote?; let imageURL: URL?; let empty: (String, String) }

struct Provider: TimelineProvider {
    private let reader = WidgetFeedReader(appGroup: "group.com.vocabloot.widgetbridge.sample", schemaVersion: 1)

    func placeholder(in context: Context) -> Entry { entry(at: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (Entry) -> Void) { completion(entry(at: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        let now = Date()
        let nextHour = Calendar.current.nextDate(after: now, matching: DateComponents(minute: 0), matchingPolicy: .nextTime) ?? now.addingTimeInterval(3600)
        completion(Timeline(entries: [entry(at: now)], policy: .after(nextHour)))
    }

    private func entry(at date: Date) -> Entry {
        guard let feed = reader?.read(QuoteFeed.self), !feed.payload.quotes.isEmpty else {
            return Entry(date: date, quote: nil, imageURL: nil, empty: ("Quote of the day", "Open the app once"))
        }
        let nowMs = Int64(date.timeIntervalSince1970 * 1000)
        let featured = feed.payload.featuredId.flatMap { fid in (feed.payload.featuredUntilEpochMs ?? 0) > nowMs ? feed.payload.quotes.firstIndex { $0.id == fid } : nil }
        let index = featured ?? WidgetRotation.index(slot: WidgetRotation.hourSlot(date), count: feed.payload.quotes.count)
        let quote = feed.payload.quotes[index]
        return Entry(date: date, quote: quote, imageURL: quote.image.flatMap(feed.assetURL), empty: (feed.payload.emptyTitle, feed.payload.emptyBody))
    }
}

struct QuoteWidgetView: View {
    let entry: Entry
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let quote = entry.quote {
                if let url = entry.imageURL, let image = UIImage(contentsOfFile: url.path) { Image(uiImage: image).resizable().scaledToFit().frame(maxHeight: 60) }
                Text(quote.text).font(.headline).lineLimit(3)
                Text(quote.author).font(.caption).foregroundStyle(.secondary)
            } else {
                Text(entry.empty.0).font(.headline)
                Text(entry.empty.1).font(.caption)
            }
        }
        .padding(12)
        .modifier(WidgetBackground())
    }
}

struct WidgetBackground: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content.containerBackground(for: .widget) { Color(.systemBackground) }
        } else {
            content.background(Color(.systemBackground))
        }
    }
}

@main
struct QuoteWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "QuoteWidget", provider: Provider()) { QuoteWidgetView(entry: $0) }
            .configurationDisplayName("Quote of the day")
            .supportedFamilies([.systemSmall, .systemMedium])
    }
}
