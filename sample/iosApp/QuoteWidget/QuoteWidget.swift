import SwiftUI
import WidgetKit
import WidgetBridge

struct Quote: Decodable { let id: String; let text: String; let author: String; let image: String? }
struct QuoteFeed: Decodable { let quotes: [Quote]; let emptyTitle: String; let emptyBody: String; let featuredId: String?; let featuredUntilEpochMs: Int64? }

private let cream = Color(red: 0.98, green: 0.95, blue: 0.88)
private let ink = Color(red: 0.15, green: 0.13, blue: 0.13)
private let secondaryInk = Color(red: 0.40, green: 0.36, blue: 0.34)
private let coral = Color(red: 1.00, green: 0.44, blue: 0.33)

struct Entry: TimelineEntry {
    let date: Date
    let quote: Quote?
    let imageURL: URL?
    let position: Int
    let total: Int
    let empty: (String, String)
}

/// A slideshow: one timeline entry per hour boundary for the next day, each pointing at the
/// quote `WidgetRotation` picks for that hour. WidgetKit flips entries on time with no refresh
/// budget spent; `publish` in the app reloads the timeline when the feed changes.
struct Provider: TimelineProvider {
    private let reader = WidgetFeedReader(appGroup: "group.com.vocabloot.widgetbridge.sample", schemaVersion: 1)

    func placeholder(in context: Context) -> Entry { entry(at: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (Entry) -> Void) { completion(entry(at: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        let now = Date()
        let calendar = Calendar.autoupdatingCurrent
        let firstBoundary = calendar.dateInterval(of: .hour, for: now)?.end ?? now.addingTimeInterval(3600)
        var dates = [now]
        for offset in 0..<24 {
            if let date = calendar.date(byAdding: .hour, value: offset, to: firstBoundary) { dates.append(date) }
        }
        completion(Timeline(entries: dates.map(entry(at:)), policy: .atEnd))
    }

    private func entry(at date: Date) -> Entry {
        guard let feed = reader?.read(QuoteFeed.self), !feed.payload.quotes.isEmpty else {
            return Entry(date: date, quote: nil, imageURL: nil, position: 0, total: 0, empty: ("Quote of the day", "Open the app once"))
        }
        let nowMs = Int64(date.timeIntervalSince1970 * 1000)
        let featured = feed.payload.featuredId.flatMap { fid in (feed.payload.featuredUntilEpochMs ?? 0) > nowMs ? feed.payload.quotes.firstIndex { $0.id == fid } : nil }
        let index = featured ?? WidgetRotation.index(slot: WidgetRotation.hourSlot(date), count: feed.payload.quotes.count)
        let quote = feed.payload.quotes[index]
        return Entry(date: date, quote: quote, imageURL: quote.image.flatMap(feed.assetURL), position: index + 1, total: feed.payload.quotes.count, empty: (feed.payload.emptyTitle, feed.payload.emptyBody))
    }
}

struct QuoteWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: Entry

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            content.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            if entry.total > 1 {
                Text("\(entry.position) / \(entry.total)")
                    .font(.system(size: 10, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(coral, in: Capsule())
            }
        }
        .modifier(WidgetBackground())
    }

    @ViewBuilder private var content: some View {
        if let quote = entry.quote {
            if family == .systemSmall {
                VStack(alignment: .leading, spacing: 6) {
                    cover(width: 56, height: 56, radius: 12)
                    Text(quote.text).font(.system(size: 12, weight: .bold, design: .rounded)).foregroundStyle(ink).lineLimit(3)
                    Text(quote.author).font(.system(size: 10, weight: .medium, design: .rounded)).foregroundStyle(secondaryInk).lineLimit(1)
                }
            } else {
                HStack(alignment: .center, spacing: 12) {
                    cover(width: 76, height: 96, radius: 14)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(quote.text).font(.system(size: 15, weight: .bold, design: .rounded)).foregroundStyle(ink).lineLimit(3)
                        Text(quote.author).font(.system(size: 12, weight: .medium, design: .rounded)).foregroundStyle(secondaryInk).lineLimit(1)
                    }
                    Spacer(minLength: 0)
                }
            }
        } else {
            VStack(alignment: .leading, spacing: 4) {
                Text(entry.empty.0).font(.system(size: 16, weight: .bold, design: .rounded)).foregroundStyle(ink)
                Text(entry.empty.1).font(.system(size: 12, weight: .medium, design: .rounded)).foregroundStyle(secondaryInk)
            }
        }
    }

    @ViewBuilder private func cover(width: CGFloat, height: CGFloat, radius: CGFloat) -> some View {
        if let url = entry.imageURL, let image = UIImage(contentsOfFile: url.path) {
            Image(uiImage: image).resizable().scaledToFill()
                .frame(width: width, height: height)
                .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
        } else {
            RoundedRectangle(cornerRadius: radius, style: .continuous).fill(coral.opacity(0.25)).frame(width: width, height: height)
        }
    }
}

struct WidgetBackground: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content.containerBackground(for: .widget) { cream }
        } else {
            content.padding(12).background(cream)
        }
    }
}

@main
struct QuoteWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "QuoteWidget", provider: Provider()) { QuoteWidgetView(entry: $0) }
            .configurationDisplayName("Quote of the day")
            .description("A new quote every hour.")
            .supportedFamilies([.systemSmall, .systemMedium])
    }
}
