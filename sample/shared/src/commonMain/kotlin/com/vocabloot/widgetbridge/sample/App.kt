package com.vocabloot.widgetbridge.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vocabloot.widgetbridge.PublishResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
/** [onPinWidget] is Android's "Add widget to home screen" request; null on iOS, where the user adds widgets from the home screen. */
@Composable
fun App(onPinWidget: (() -> Unit)? = null) {
    val store = remember { QuoteStore() }
    val bridge = remember { runCatching { createQuoteBridge() }.getOrNull() }
    val quotes by store.quotes.collectAsState()
    var text by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not published yet") }

    // The whole "refresh policy": republish, debounced, whenever the data changes.
    LaunchedEffect(Unit) {
        store.attachImages(withContext(Dispatchers.Default) { SampleImages.materialize(cacheDirectory()) })
        combine(store.quotes, store.featured) { _, _ -> store.feed() }
            .debounce(500.milliseconds)
            .collect { feed ->
                status = when (val result = runCatching { withContext(Dispatchers.Default) { bridge?.publish(feed, store.assets()) } }.getOrElse { it }) {
                    is PublishResult.Published -> "Published ${result.generationId} (${feed.quotes.size} quotes, ${result.assetBytes / 1024} KB of images)"
                    PublishResult.Unchanged -> "Unchanged"
                    null -> "Bridge unavailable: check the App Group entitlement"
                    is Throwable -> "Publish failed: ${result.message}"
                    else -> status
                }
            }
    }

    MaterialTheme {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("WidgetBridge sample", style = MaterialTheme.typography.headlineSmall)
                Text(status, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Quote") })
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = author, onValueChange = { author = it }, modifier = Modifier.weight(1f), label = { Text("Author") })
                    Button(
                        onClick = { if (text.isNotBlank()) { store.add(text, author); text = ""; author = "" } },
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Add") }
                }
                Spacer(Modifier.height(12.dp))
                onPinWidget?.let { pin ->
                    Button(onClick = pin, modifier = Modifier.fillMaxWidth()) { Text("Add widget to home screen") }
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(quotes, key = { it.id }) { quote ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(quote.text)
                            Text(quote.author, style = MaterialTheme.typography.bodySmall)
                            Row {
                                TextButton(onClick = { store.feature(quote.id, nowEpochMs() + 3_600_000L) }) { Text("Feature for 1 hour") }
                                TextButton(onClick = { store.remove(quote.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
