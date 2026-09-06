package com.vocabloot.widgetbridge.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vocabloot.widgetbridge.WidgetBridge
import com.vocabloot.widgetbridge.WidgetBridgeConfig
import com.vocabloot.widgetbridge.WidgetRotation
import com.vocabloot.widgetbridge.androidWidgetBridge
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.ZoneId

private val Cream = Color(0xFFFAF2E0)
private val Ink = Color(0xFF262121)
private val SecondaryInk = Color(0xFF665C57)
private val Coral = Color(0xFFFF7054)

/**
 * A slideshow card. Every hour the widget moves to the next quote (see `WidgetRotation`); the
 * launcher redraws it on `updatePeriodMillis` from `quote_widget_info.xml`, and `publish` redraws
 * it immediately when the app changes the feed.
 */
class QuoteWidget : GlanceAppWidget() {

    companion object {
        /** Bumped by [QuoteWidgetReceiver] on every update broadcast so a live composition re-reads the feed. */
        val refreshes = MutableStateFlow(0)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(DpSize(140.dp, 140.dp), DpSize(280.dp, 140.dp)))

    private data class Model(val quote: Quote?, val image: Bitmap?, val position: Int, val total: Int, val emptyTitle: String, val emptyBody: String)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bridge = androidWidgetBridge(context, WidgetBridgeConfig(schemaVersion = QUOTE_SCHEMA_VERSION), QuoteFeed.serializer(), QuoteWidgetReceiver::class.java)
        provideContent {
            // Re-read the feed on every update broadcast, not only when this session starts.
            val refresh by refreshes.collectAsState()
            val model = remember(refresh) { loadModel(bridge) }
            QuoteCard(model)
        }
    }

    private fun loadModel(bridge: WidgetBridge<QuoteFeed>): Model {
        val feed = bridge.read()
        val now = System.currentTimeMillis()
        if (feed == null || feed.payload.quotes.isEmpty()) {
            return Model(null, null, 0, 0, feed?.payload?.emptyTitle ?: "Quote of the day", feed?.payload?.emptyBody ?: "Open the app once")
        }
        val payload = feed.payload
        val featured = payload.featuredId?.takeIf { (payload.featuredUntilEpochMs ?: 0L) > now }
            ?.let { fid -> payload.quotes.indexOfFirst { it.id == fid } }?.takeIf { it >= 0 }
        val offset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(now)).totalSeconds
        val index = featured ?: WidgetRotation.index(WidgetRotation.hourSlot(now, offset), payload.quotes.size)
        val quote = payload.quotes[index]
        val image = quote.image?.let { feed.assetPath(it) }?.let(BitmapFactory::decodeFile)
        return Model(quote, image, index + 1, payload.quotes.size, payload.emptyTitle, payload.emptyBody)
    }

    @Composable
    private fun QuoteCard(model: Model) {
        val wide = LocalSize.current.width >= 200.dp
        Box(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Cream)).cornerRadius(20.dp)
                .clickable(actionStartActivity(MainActivity::class.java)),
        ) {
            val quote = model.quote
            if (quote == null) {
                Column(GlanceModifier.fillMaxSize().padding(14.dp)) {
                    Text(model.emptyTitle, style = TextStyle(color = ColorProvider(Ink), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    Text(model.emptyBody, style = TextStyle(color = ColorProvider(SecondaryInk), fontSize = 12.sp))
                }
            } else if (wide) {
                Row(GlanceModifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(model.image, width = 76.dp, height = 96.dp)
                    Spacer(GlanceModifier.width(12.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(quote.text, style = TextStyle(color = ColorProvider(Ink), fontWeight = FontWeight.Bold, fontSize = 15.sp), maxLines = 3)
                        Spacer(GlanceModifier.height(2.dp))
                        Text(quote.author, style = TextStyle(color = ColorProvider(SecondaryInk), fontSize = 12.sp), maxLines = 1)
                    }
                }
            } else {
                Column(GlanceModifier.fillMaxSize().padding(12.dp)) {
                    Cover(model.image, width = 56.dp, height = 56.dp)
                    Spacer(GlanceModifier.height(6.dp))
                    Text(quote.text, style = TextStyle(color = ColorProvider(Ink), fontWeight = FontWeight.Bold, fontSize = 12.sp), maxLines = 3)
                    Text(quote.author, style = TextStyle(color = ColorProvider(SecondaryInk), fontSize = 10.sp), maxLines = 1)
                }
            }
            if (model.total > 1) {
                Box(GlanceModifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(GlanceModifier.background(ColorProvider(Coral)).cornerRadius(999.dp).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("${model.position} / ${model.total}", style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }
            }
        }
    }

    @Composable
    private fun Cover(bitmap: Bitmap?, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
        val shape = GlanceModifier.size(width, height).cornerRadius(14.dp)
        if (bitmap != null) {
            Image(provider = ImageProvider(bitmap), contentDescription = null, modifier = shape, contentScale = ContentScale.Crop)
        } else {
            Box(shape.background(ColorProvider(Coral.copy(alpha = 0.25f)))) {}
        }
    }
}
