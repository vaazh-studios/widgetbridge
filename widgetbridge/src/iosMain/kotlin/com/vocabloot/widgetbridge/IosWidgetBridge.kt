package com.vocabloot.widgetbridge

import kotlinx.serialization.KSerializer

/** Default iOS wiring: App Group storage plus a NotificationCenter post. Requires [WidgetBridgeConfig.iosAppGroup]. */
public fun <T> iosWidgetBridge(config: WidgetBridgeConfig, serializer: KSerializer<T>): WidgetBridge<T> {
    val appGroup = requireNotNull(config.iosAppGroup) { "WidgetBridgeConfig.iosAppGroup is required on iOS" }
    return WidgetBridge(
        config = config,
        serializer = serializer,
        storage = IosWidgetFeedStorage(appGroup = appGroup, directoryName = config.directoryName),
        notifier = NotificationCenterRefreshNotifier(),
    )
}
