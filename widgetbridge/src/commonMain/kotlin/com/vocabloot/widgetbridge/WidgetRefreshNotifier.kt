package com.vocabloot.widgetbridge

/** Tells the OS that a new generation is current so widgets redraw. Platform defaults exist; [None] for tests. */
public fun interface WidgetRefreshNotifier {
    public fun notifyChanged()

    public companion object {
        public val None: WidgetRefreshNotifier = WidgetRefreshNotifier { }
    }
}
