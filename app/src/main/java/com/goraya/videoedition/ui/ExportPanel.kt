package com.goraya.videoedition.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goraya.videoedition.export.Exporter

/** Output size for a given short side and aspect ratio; both sides are made even for the encoder. */
fun exportSize(shortSide: Int, aw: Int, ah: Int): Pair<Int, Int> {
    val w: Int
    val h: Int
    if (aw >= ah) { w = shortSide * aw / ah; h = shortSide } else { w = shortSide; h = shortSide * ah / aw }
    return Pair(w / 2 * 2, h / 2 * 2)
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ExportPanel(exporter: Exporter, hasClips: Boolean, onStart: (Int, Int) -> Unit) {
    val resolutions = listOf(480, 720, 1080)
    val aspects = listOf("16:9" to (16 to 9), "9:16" to (9 to 16), "1:1" to (1 to 1), "4:5" to (4 to 5))
    var res by remember { mutableIntStateOf(720) }
    var aspectIdx by remember { mutableIntStateOf(0) }
    val (aw, ah) = aspects[aspectIdx].second
    val (w, h) = exportSize(res, aw, ah)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            resolutions.forEach { r -> Choice("${r}p", r == res) { res = r } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            aspects.forEachIndexed { i, a -> Choice(a.first, i == aspectIdx) { aspectIdx = i } }
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
