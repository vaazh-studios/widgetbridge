package com.vocabloot.widgetbridge

import android.content.Context
import kotlinx.serialization.KSerializer

/** Default Android wiring: files-dir storage plus an app-widget update broadcast to [receiverClass]. */
public fun <T> androidWidgetBridge(
    context: Context,
    config: WidgetBridgeConfig,
    serializer: KSerializer<T>,
    receiverClass: Class<*>,
): WidgetBridge<T> = WidgetBridge(
    config = config,
    serializer = serializer,
    storage = AndroidWidgetFeedStorage.forApp(context.applicationContext, config.directoryName),
    notifier = AppWidgetRefreshNotifier(context.applicationContext, receiverClass),
)
