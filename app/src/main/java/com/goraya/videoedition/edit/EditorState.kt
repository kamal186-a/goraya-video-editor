package com.goraya.videoedition.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.goraya.videoedition.media.MediaAsset

const val PHOTO_DEFAULT_MS = 3000L
const val PHOTO_MAX_MS = 10000L
const val MIN_CLIP_MS = 300L

/** One clip on the main track. For photos only (outMs - inMs) matters: it is the display duration. */
data class EditClip(
    val id: Long,
    val asset: MediaAsset,
    val inMs: Long,
    val outMs: Long,
    val look: Look = Look(),
    val texts: List<TextItem> = emptyList(),
    val muted: Boolean = false
) {
    val durationMs: Long get() = outMs - inMs
    val maxLenMs: Long get() = if (asset.isVideo) asset.durationMs else PHOTO_MAX_MS
}

class EditorState {
    val clips = mutableStateListOf<EditClip>()
    var selectedId by mutableLongStateOf(-1L)
    private var nextId = 1L

    val selected: EditClip? get() = clips.firstOrNull { it.id == selectedId }
    val totalMs: Long get() = clips.sumOf { it.durationMs }

    private fun indexOfSelected() = clips.indexOfFirst { it.id == selectedId }

    fun add(asset: MediaAsset) {
        val c = EditClip(nextId++, asset, 0L, if (asset.isVideo) asset.durationMs else PHOTO_DEFAULT_MS)
        clips.add(c)
        if (selected == null) selectedId = c.id
    }

    var music by mutableStateOf<MusicTrack?>(null)

    fun toggleMute() {
        val i = indexOfSelected()
        if (i < 0) return
        clips[i] = clips[i].copy(muted = !clips[i].muted)
    }

    /** Emoji sticker: a text layer holding one emoji, drawn by the same renderer. */
    fun addSticker(emoji: String): Long {
        val i = indexOfSelected()
        if (i < 0) return -1L
        val t = TextItem(nextTextId++, text = emoji, size = 0.18f, bold = false)
        clips[i] = clips[i].copy(texts = clips[i].texts + t)
        return t.id
    }

    fun restore(list: List<EditClip>, m: MusicTrack?, c: CanvasOption) {
        clips.clear()
        clips.addAll(list)
        music = m
        canvas = c
        nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1L
        nextTextId = (list.flatMap { it.texts }.maxOfOrNull { it.id } ?: 0L) + 1L
        selectedId = list.firstOrNull()?.id ?: -1L
    }

    var canvas by mutableStateOf(CANVAS_OPTIONS[0])
    private var nextTextId = 1L

    /** Adds a text layer to the selected clip; returns its id or -1. */
    fun addText(): Long {
        val i = indexOfSelected()
        if (i < 0) return -1L
        val t = TextItem(nextTextId++)
        clips[i] = clips[i].copy(texts = clips[i].texts + t)
        return t.id
    }

    fun updateText(id: Long, change: (TextItem) -> TextItem) {
        val i = indexOfSelected()
        if (i < 0) return
        val c = clips[i]
        clips[i] = c.copy(texts = c.texts.map { if (it.id == id) change(it) else it })
    }

    fun deleteText(id: Long) {
        val i = indexOfSelected()
        if (i < 0) return
        val c = clips[i]
        clips[i] = c.copy(texts = c.texts.filter { it.id != id })
    }

    fun select(id: Long) { selectedId = id }

    fun delete() {
        val i = indexOfSelected()
        if (i < 0) return
        clips.removeAt(i)
        selectedId = clips.getOrNull(minOf(i, clips.size - 1))?.id ?: -1L
    }

    fun duplicate() {
        val i = indexOfSelected()
        if (i < 0) return
        val copy = clips[i].copy(id = nextId++)
        clips.add(i + 1, copy)
        selectedId = copy.id
    }

    fun move(delta: Int) {
        val i = indexOfSelected()
        val j = i + delta
        if (i < 0 || j < 0 || j >= clips.size) return
        val c = clips.removeAt(i)
        clips.add(j, c)
    }

    /** Splits the selected clip at [localMs] (time inside the clip). Returns false if too close to an edge. */
    fun split(localMs: Long): Boolean {
        val i = indexOfSelected()
        if (i < 0) return false
        val c = clips[i]
        if (localMs < MIN_CLIP_MS || c.durationMs - localMs < MIN_CLIP_MS) return false
        val cut = c.inMs + localMs
        val a = c.copy(outMs = cut)
        val b = c.copy(id = nextId++, inMs = cut)
        clips[i] = a
        clips.add(i + 1, b)
        selectedId = b.id
        return true
    }

    fun setLook(look: Look) {
        val i = indexOfSelected()
        if (i < 0) return
        clips[i] = clips[i].copy(look = look)
    }

    fun applyLookToAll(look: Look) {
        for (i in clips.indices) clips[i] = clips[i].copy(look = look)
    }

    fun trim(newIn: Long, newOut: Long) {
        val i = indexOfSelected()
        if (i < 0) return
        val c = clips[i]
        val hi = maxOf(0L, c.maxLenMs - MIN_CLIP_MS)
        val s = newIn.coerceIn(0L, hi)
        val e = newOut.coerceIn(s + MIN_CLIP_MS, maxOf(c.maxLenMs, s + MIN_CLIP_MS))
        clips[i] = c.copy(inMs = s, outMs = e)
    }
}
