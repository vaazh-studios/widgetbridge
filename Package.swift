// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "WidgetBridge",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [.library(name: "WidgetBridge", targets: ["WidgetBridge"])],
    targets: [
        .target(name: "WidgetBridge", path: "Sources/WidgetBridge"),
        .testTarget(name: "WidgetBridgeTests", dependencies: ["WidgetBridge"], path: "Tests/WidgetBridgeTests"),
    ]
)
