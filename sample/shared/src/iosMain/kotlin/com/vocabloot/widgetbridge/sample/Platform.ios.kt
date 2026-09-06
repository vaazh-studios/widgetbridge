package com.vocabloot.widgetbridge.sample

import com.vocabloot.widgetbridge.WidgetBridge
import com.vocabloot.widgetbridge.WidgetBridgeConfig
import com.vocabloot.widgetbridge.iosWidgetBridge
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

const val SAMPLE_APP_GROUP = "group.com.vocabloot.widgetbridge.sample"

actual fun createQuoteBridge(): WidgetBridge<QuoteFeed> = iosWidgetBridge(
    config = WidgetBridgeConfig(schemaVersion = QUOTE_SCHEMA_VERSION, iosAppGroup = SAMPLE_APP_GROUP),
    serializer = QuoteFeed.serializer(),
)

actual fun nowEpochMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
