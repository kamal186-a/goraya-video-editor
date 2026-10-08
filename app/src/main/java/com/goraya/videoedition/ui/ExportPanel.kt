package com.goraya.videoedition.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goraya.videoedition.edit.CanvasOption
import com.goraya.videoedition.export.Exporter

/** Output size for a given short side and aspect ratio; both sides are made even for the encoder. */
fun exportSize(shortSide: Int, aw: Int, ah: Int): Pair<Int, Int> {
    val w: Int
    val h: Int
    if (aw >= ah) { w = shortSide * aw / ah; h = shortSide } else { w = shortSide; h = shortSide * ah / aw }
    return Pair(w / 2 * 2, h / 2 * 2)
}

@Composable
fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ExportPanel(exporter: Exporter, hasClips: Boolean, canvas: CanvasOption, onStart: (Int, Int) -> Unit) {
    val resolutions = listOf(480, 720, 1080)
    var res by remember { mutableIntStateOf(720) }
    val (w, h) = exportSize(res, canvas.aw, canvas.ah)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            resolutions.forEach { r -> Choice("${r}p", r == res) { res = r } }
            Text("Canvas: ${canvas.label}")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (exporter.running) {
                Button(onClick = { exporter.cancel() }) { Text("Cancel") }
                Spacer(Modifier.width(12.dp))
                Text("${exporter.progress}%")
            } else {
                Button(onClick = { onStart(w, h) }, enabled = hasClips) { Text("Export MP4 (${w}x$h)") }
            }
        }
        if (exporter.running) {
            LinearProgressIndicator(progress = { exporter.progress / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
        exporter.message?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
    }
}
