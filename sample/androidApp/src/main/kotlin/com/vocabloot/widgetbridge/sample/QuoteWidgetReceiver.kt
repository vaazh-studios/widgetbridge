package com.vocabloot.widgetbridge.sample

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()

    override fun onReceive(context: Context, intent: Intent) {
        // Glance keeps a widget composition alive for a while after it renders, and an update on a
        // live session only recomposes: code that ran before provideContent does not run again.
        // Bumping this counter makes the composition re-read the feed (see QuoteWidget).
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) QuoteWidget.refreshes.value++
        super.onReceive(context, intent)
    }
}
