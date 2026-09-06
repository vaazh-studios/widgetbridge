package com.vocabloot.widgetbridge

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Broadcasts `ACTION_APPWIDGET_UPDATE` to [receiverClass] for every placed widget. No Glance dependency. */
public class AppWidgetRefreshNotifier(private val context: Context, private val receiverClass: Class<*>) : WidgetRefreshNotifier {
    override fun notifyChanged() {
        val component = ComponentName(context, receiverClass)
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(component)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setComponent(component).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
        )
    }
}
