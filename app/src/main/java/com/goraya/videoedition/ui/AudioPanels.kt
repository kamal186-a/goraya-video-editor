package com.goraya.videoedition.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.goraya.videoedition.edit.EditClip
import com.goraya.videoedition.edit.EditorState

@Composable
fun AudioPanel(state: EditorState, onPickMusic: () -> Unit) {
    val m = state.music
    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onPickMusic) { Text(if (m == null) "Music add" else "Music badlen") }
        if (m != null) TextButton(onClick = { state.music = null }) { Text("Music hatao") }
        val c = state.selected
        if (c != null && c.asset.isVideo) {
            Choice(if (c.muted) "Video awaaz: band" else "Video awaaz: chalu", !c.muted) { state.toggleMute() }
        }
    }
    if (m == null) {
        Text("Music export mein video ke sath mix hoti hai (preview mein sirf video ki awaaz aati hai).", fontSize = 12.sp)
        return
    }
    val key = m.uri.toString()
    Text(m.name + " (" + formatTime(m.durationMs) + ")", fontSize = 12.sp)
    LabeledSlider("Volume", m.volume, 0f..1.5f, key) { state.music = m.copy(volume = it) }
    LabeledSlider("Fade in (sec)", m.fadeInMs / 1000f, 0f..5f, key) { state.music = m.copy(fadeInMs = (it * 1000).toLong()) }
    LabeledSlider("Fade out (sec)", m.fadeOutMs / 1000f, 0f..5f, key) { state.music = m.copy(fadeOutMs = (it * 1000).toLong()) }
}

private val emojis = listOf(
    "😀", "😍", "😂", "🔥", "❤️", "⭐", "✨", "🎉",
    "👍", "👑", "💯", "🌈", "🌸", "🎵", "⚡", "💥",
    "🎬", "📍", "☀️", "🌙", "🦋", "🍀", "💎", "✅"
)

@Composable
fun StickerPanel(clip: EditClip, state: EditorState) {
    Text("Sticker chunen, phir Text tool mein size aur jagah badlen.", fontSize = 12.sp)
    emojis.chunked(8).forEach { row ->
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            row.forEach { e -> TextButton(onClick = { state.addSticker(e) }) { Text(e, fontSize = 22.sp) } }
        }
    }
    Text("Is clip par ${clip.texts.size} text/sticker hain.", fontSize = 12.sp)
}
