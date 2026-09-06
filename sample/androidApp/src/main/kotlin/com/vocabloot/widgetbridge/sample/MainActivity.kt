package com.vocabloot.widgetbridge.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformHolder.appContext = applicationContext
        PlatformHolder.receiverClass = QuoteWidgetReceiver::class.java
        setContent { App() }
    }
}
