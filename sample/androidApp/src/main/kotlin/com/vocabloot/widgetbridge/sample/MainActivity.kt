package com.vocabloot.widgetbridge.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformHolder.appContext = applicationContext
        PlatformHolder.receiverClass = QuoteWidgetReceiver::class.java
        setContent {
            App(onPinWidget = { lifecycleScope.launch { GlanceAppWidgetManager(this@MainActivity).requestPinGlanceAppWidget(QuoteWidgetReceiver::class.java) } })
        }
    }
}
