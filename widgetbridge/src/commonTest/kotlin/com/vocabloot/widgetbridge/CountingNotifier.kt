package com.vocabloot.widgetbridge

class CountingNotifier : WidgetRefreshNotifier {
    var count = 0
    override fun notifyChanged() { count += 1 }
}
