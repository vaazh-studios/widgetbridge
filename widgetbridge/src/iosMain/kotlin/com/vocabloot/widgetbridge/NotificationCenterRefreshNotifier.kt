package com.vocabloot.widgetbridge

import platform.Foundation.NSNotificationCenter

/** Name observed by the Swift package's `WidgetBridgeReloader`, which calls `WidgetCenter.reloadAllTimelines()`. */
public const val WIDGET_BRIDGE_FEED_CHANGED: String = "WidgetBridgeFeedChanged"

public class NotificationCenterRefreshNotifier : WidgetRefreshNotifier {
    override fun notifyChanged() {
        NSNotificationCenter.defaultCenter.postNotificationName(aName = WIDGET_BRIDGE_FEED_CHANGED, `object` = null)
    }
}
