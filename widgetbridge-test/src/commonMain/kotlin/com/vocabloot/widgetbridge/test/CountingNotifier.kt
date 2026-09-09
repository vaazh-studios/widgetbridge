package com.vocabloot.widgetbridge.test

import com.vocabloot.widgetbridge.WidgetRefreshNotifier

/** [WidgetRefreshNotifier] that only counts: assert how many times a publish asked the OS to redraw. */
public class CountingNotifier : WidgetRefreshNotifier {
    public var count: Int = 0

    override fun notifyChanged() {
        count += 1
    }
}
