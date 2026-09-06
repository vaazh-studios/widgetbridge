package com.vocabloot.widgetbridge.sample

import android.content.Context
import com.vocabloot.widgetbridge.WidgetBridge
import com.vocabloot.widgetbridge.WidgetBridgeConfig
import com.vocabloot.widgetbridge.androidWidgetBridge

/** The app module sets these before showing UI; the shared module cannot see the receiver class. */
object PlatformHolder {
    lateinit var appContext: Context
    lateinit var receiverClass: Class<*>
}

actual fun createQuoteBridge(): WidgetBridge<QuoteFeed> = androidWidgetBridge(
    context = PlatformHolder.appContext,
    config = WidgetBridgeConfig(schemaVersion = QUOTE_SCHEMA_VERSION),
    serializer = QuoteFeed.serializer(),
    receiverClass = PlatformHolder.receiverClass,
)

actual fun nowEpochMs(): Long = System.currentTimeMillis()
