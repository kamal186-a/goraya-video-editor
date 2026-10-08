package com.goraya.videoedition.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goraya.videoedition.edit.EditorState
import com.goraya.videoedition.media.MediaAsset
import com.goraya.videoedition.media.MediaLoader

@Composable
fun Thumb(asset: MediaAsset) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, asset) { value = MediaLoader.thumbnail(ctx, asset, 256) }
    bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
}

@Composable
fun TimelineView(state: EditorState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Timeline: ${state.clips.size} clips, kul ${formatTime(state.totalMs)}", fontSize = 12.sp)
        Row(
            Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.clips.isEmpty()) {
                Text("  Media tool se Import dabayen", fontSize = 12.sp)
            }
            for (c in state.clips) {
                val sel = c.id == state.selectedId
                val shape = RoundedCornerShape(6.dp)
                val w = ((c.durationMs / 1000f) * 32f).dp.coerceAtLeast(56.dp).coerceAtMost(320.dp)
                Box(
                    Modifier.width(w).fillMaxHeight().clip(shape)
                        .border(2.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, shape)
                        .clickable { state.select(c.id) }
                ) {
                    Thumb(c.asset)
                    Text(
                        (if (c.asset.isVideo) "" else "IMG ") + formatTime(c.durationMs),
                        Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 4.dp),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
