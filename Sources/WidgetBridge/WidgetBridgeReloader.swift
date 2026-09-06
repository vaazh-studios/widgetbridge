import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

/// App target only. Observes the notification the Kotlin `NotificationCenterRefreshNotifier` posts and reloads all timelines.
public enum WidgetBridgeReloader {
    public static let notificationName = Notification.Name("WidgetBridgeFeedChanged")
    private static var token: NSObjectProtocol?

    public static func start() {
        guard token == nil else { return }
        token = NotificationCenter.default.addObserver(forName: notificationName, object: nil, queue: .main) { _ in
            #if canImport(WidgetKit)
            if #available(iOS 14.0, macOS 11.0, *) { WidgetCenter.shared.reloadAllTimelines() }
            #endif
        }
    }
}
