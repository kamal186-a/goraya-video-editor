package com.goraya.videoedition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goraya.videoedition.edit.EditClip
import com.goraya.videoedition.edit.EditorState
import com.goraya.videoedition.edit.CANVAS_OPTIONS

private val swatches = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF3B30, 0xFFFFCC00, 0xFF34C759, 0xFF0A84FF, 0xFFFF2D92
).map { it.toInt() }

private val fonts = listOf("Sans", "Serif", "Mono")

@Composable
fun CanvasPanel(state: EditorState) {
    Text("Canvas (video ka aspect ratio)")
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        CANVAS_OPTIONS.forEach { o -> Choice(o.label, o == state.canvas) { state.canvas = o } }
    }
}

@Composable
fun TextPanel(clip: EditClip, state: EditorState) {
    var sel by remember(clip.id) { mutableStateOf<Long?>(clip.texts.firstOrNull()?.id) }
    val cur = clip.texts.firstOrNull { it.id == sel }

    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { val id = state.addText(); if (id > 0) sel = id }) { Text("Text add") }
        clip.texts.forEachIndexed { i, t -> Choice("T${i + 1}", t.id == sel) { sel = t.id } }
        if (cur != null) TextButton(onClick = { state.deleteText(cur.id); sel = null }) { Text("Delete") }
    }
    if (cur == null) {
        Text("Text yeh clip ke poore dauraniye mein nazar aata hai. 'Text add' dabayen.")
        return
    }

    OutlinedTextField(
        value = cur.text,
        onValueChange = { v -> state.updateText(cur.id) { it.copy(text = v) } },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3,
        label = { Text("Text (Urdu / English)") }
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        fonts.forEachIndexed { i, n -> Choice(n, cur.font == i) { state.updateText(cur.id) { it.copy(font = i) } } }
        Choice("Bold", cur.bold) { state.updateText(cur.id) { it.copy(bold = !it.bold) } }
        Choice("Italic", cur.italic) { state.updateText(cur.id) { it.copy(italic = !it.italic) } }
        Choice("Background", cur.background) { state.updateText(cur.id) { it.copy(background = !it.background) } }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        swatches.forEach { c ->
            Box(
                Modifier.padding(end = 8.dp).size(28.dp).background(Color(c), CircleShape)
                    .border(2.dp, if (cur.color == c) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                    .clickable { state.updateText(cur.id) { it.copy(color = c) } }
            )
        }
    }
    LabeledSlider("Size", cur.size, 0.03f..0.20f, cur.id) { v -> state.updateText(cur.id) { it.copy(size = v) } }
    LabeledSlider("Left-Right", cur.cx, 0f..1f, cur.id) { v -> state.updateText(cur.id) { it.copy(cx = v) } }
    LabeledSlider("Upar-Neeche", cur.cy, 0f..1f, cur.id) { v -> state.updateText(cur.id) { it.copy(cy = v) } }
    LabeledSlider("Opacity", cur.opacity, 0.1f..1f, cur.id) { v -> state.updateText(cur.id) { it.copy(opacity = v) } }
    LabeledSlider("Rotation", cur.rotation, -180f..180f, cur.id) { v -> state.updateText(cur.id) { it.copy(rotation = v) } }
}
