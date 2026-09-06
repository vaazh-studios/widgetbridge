import SwiftUI
import WidgetBridge

@main
struct iOSApp: App {
    init() { WidgetBridgeReloader.start() }

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
