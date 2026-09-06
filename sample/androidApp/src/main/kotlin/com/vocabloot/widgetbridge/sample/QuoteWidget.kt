package com.vocabloot.widgetbridge.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.vocabloot.widgetbridge.WidgetBridgeConfig
import com.vocabloot.widgetbridge.WidgetRotation
import com.vocabloot.widgetbridge.androidWidgetBridge
import java.time.Instant
import java.time.ZoneId

class QuoteWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(DpSize(140.dp, 140.dp), DpSize(280.dp, 140.dp)))

    private data class Model(val quote: Quote?, val image: Bitmap?, val emptyTitle: String, val emptyBody: String)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bridge = androidWidgetBridge(context, WidgetBridgeConfig(schemaVersion = QUOTE_SCHEMA_VERSION), QuoteFeed.serializer(), QuoteWidgetReceiver::class.java)
        val feed = bridge.read()
        val now = System.currentTimeMillis()
        val model = if (feed == null || feed.payload.quotes.isEmpty()) {
            Model(null, null, feed?.payload?.emptyTitle ?: "Quote of the day", feed?.payload?.emptyBody ?: "Open the app once")
        } else {
            val payload = feed.payload
            val featured = payload.featuredId?.takeIf { (payload.featuredUntilEpochMs ?: 0L) > now }
                ?.let { fid -> payload.quotes.indexOfFirst { it.id == fid } }?.takeIf { it >= 0 }
            val offset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(now)).totalSeconds
            val quote = payload.quotes[featured ?: WidgetRotation.index(WidgetRotation.hourSlot(now, offset), payload.quotes.size)]
            Model(quote, quote.image?.let { feed.assetPath(it) }?.let(BitmapFactory::decodeFile), payload.emptyTitle, payload.emptyBody)
        }
        provideContent { QuoteCard(model) }
    }

    @Composable
    private fun QuoteCard(model: Model) {
        Column(GlanceModifier.fillMaxSize().padding(12.dp).clickable(actionStartActivity(MainActivity::class.java))) {
            val quote = model.quote
            if (quote == null) {
                Text(model.emptyTitle, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Text(model.emptyBody, style = TextStyle(fontSize = 12.sp))
            } else {
                model.image?.let { Image(provider = ImageProvider(it), contentDescription = null, modifier = GlanceModifier.height(60.dp)) }
                Text(quote.text, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp), maxLines = 3)
                Text(quote.author, style = TextStyle(fontSize = 12.sp))
            }
        }
    }
}
