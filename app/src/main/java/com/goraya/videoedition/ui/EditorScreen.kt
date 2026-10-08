package com.goraya.videoedition.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goraya.videoedition.media.MediaAsset
import com.goraya.videoedition.media.MediaLoader
import kotlinx.coroutines.launch

private val tools = listOf("Media", "Audio", "Text", "Effects", "Filters", "Stickers", "Canvas", "Speed", "Export")

@Composable
fun EditorScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val assets = remember { mutableStateListOf<MediaAsset>() }
    var selected by remember { mutableStateOf<MediaAsset?>(null) }
    var tool by remember { mutableStateOf("Media") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        scope.launch {
            var failed = 0
            for (u in uris) {
                val r = MediaLoader.load(ctx, u)
                val a = r.getOrNull()
                if (a != null) {
                    assets.add(a)
                    if (selected == null) selected = a
                } else failed++
            }
            if (failed > 0) {
                Toast.makeText(ctx, "$failed file import nahi ho saki (unsupported ya kharab)", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Text("Goraya Video Edition", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
        PreviewPanel(
            asset = selected,
            onRemove = {
                selected?.let { assets.remove(it) }
                selected = assets.firstOrNull()
            },
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        )
        Box(
            Modifier.height(48.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) { Text("Timeline (agla hissa) - clips: ${assets.size}", fontSize = 12.sp) }

        Box(Modifier.height(110.dp).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            if (tool == "Media") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }) { Text("Import") }
                    Spacer(Modifier.width(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(assets) { a ->
                            val sel = a == selected
                            Box(
                                Modifier.size(80.dp).clip8()
                                    .border(2.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .clickable { selected = a }
                            ) {
                                Thumb(a)
                                Text(
                                    if (a.isVideo) formatTime(a.durationMs) else "IMG",
                                    Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.background).padding(horizontal = 4.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Text("$tool tool agle hisson mein aayega.")
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
            tools.forEach { t ->
                TextButton(onClick = { tool = t }) {
                    Text(t, color = if (t == tool) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private fun Modifier.clip8(): Modifier = this.then(Modifier.clip(RoundedCornerShape(8.dp)))

@Composable
private fun Thumb(asset: MediaAsset) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, asset) { value = MediaLoader.thumbnail(ctx, asset, 256) }
    bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
}
