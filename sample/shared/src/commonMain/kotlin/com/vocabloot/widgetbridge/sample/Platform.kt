package com.vocabloot.widgetbridge.sample

import com.vocabloot.widgetbridge.WidgetBridge

expect fun createQuoteBridge(): WidgetBridge<QuoteFeed>

expect fun nowEpochMs(): Long
