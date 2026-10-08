package com.goraya.videoedition.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.goraya.videoedition.edit.EditClip
import com.goraya.videoedition.edit.EditorState
import com.goraya.videoedition.edit.MIN_CLIP_MS
import com.goraya.videoedition.edit.PHOTO_MAX_MS
import com.goraya.videoedition.export.Exporter
import com.goraya.videoedition.media.AudioLoader
import com.goraya.videoedition.media.MediaLoader
import com.goraya.videoedition.project.ProjectStore
import kotlinx.coroutines.launch

private val tools = listOf("Media", "Audio", "Text", "Effects", "Filters", "Stickers", "Canvas", "Speed", "Export")

@Composable
fun EditorScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { EditorState() }
    val exporter = remember { Exporter(ctx.applicationContext) }
    DisposableEffect(exporter) { onDispose { if (exporter.running) exporter.cancel() } }
    var tool by remember { mutableStateOf("Media") }
    var previewPos by remember { mutableLongStateOf(0L) }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
            }
            val m = AudioLoader.load(ctx, uri).getOrNull()
            if (m != null) state.music = m else toast("Yeh audio file import nahi ho saki")
        }
    }

    LaunchedEffect(Unit) {
        val msg = ProjectStore.load(ctx, state)
        if (msg != null) toast(msg)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) ProjectStore.save(ctx, state) }
        lifecycleOwner.lifecycle.addObserver(o)
        onDispose { lifecycleOwner.lifecycle.removeObserver(o) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        scope.launch {
            var failed = 0
            for (u in uris) {
                try {
                    ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                }
                val a = MediaLoader.load(ctx, u).getOrNull()
                if (a != null) state.add(a) else failed++
            }
            if (failed > 0) toast("$failed file import nahi ho saki (unsupported ya kharab)")
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Goraya Video Edition", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                if (ProjectStore.save(ctx, state)) toast("Project save ho gaya") else toast("Project save nahi ho saka")
            }) { Text("Save") }
        }
        PreviewPanel(
            clip = state.selected,
            aspect = state.canvas.aw.toFloat() / state.canvas.ah,
            onPosition = { previewPos = it },
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        )
        TimelineView(state)

        Column(Modifier.height(180.dp).fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            when (tool) {
                "Media" -> {
                    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }) { Text("Import") }
                        TextButton(onClick = {
                            val c = state.selected
                            if (c != null) {
                                val local = if (c.asset.isVideo) previewPos else c.durationMs / 2
                                if (!state.split(local)) toast("Split ke liye video ko clip ke beech mein rokein")
                            }
                        }) { Text("Split") }
                        TextButton(onClick = { state.duplicate() }) { Text("Copy") }
                        TextButton(onClick = { state.delete() }) { Text("Delete") }
                        TextButton(onClick = { state.move(-1) }) { Text("< Left") }
                        TextButton(onClick = { state.move(1) }) { Text("Right >") }
                    }
                    state.selected?.let { c -> TrimPanel(c) { s, e -> state.trim(s, e) } }
                }
                "Export" -> ExportPanel(exporter, state.clips.isNotEmpty(), state.canvas) { w, h ->
                    exporter.start(state.clips.toList(), state.music, w, h)
                }
                "Audio" -> AudioPanel(state) { musicPicker.launch(arrayOf("audio/*")) }
                "Stickers" -> state.selected?.let { StickerPanel(it, state) } ?: Text("Pehle timeline se clip chunen")
                "Text" -> state.selected?.let { TextPanel(it, state) } ?: Text("Pehle timeline se clip chunen")
                "Canvas" -> CanvasPanel(state)
                "Filters" -> state.selected?.let { FiltersPanel(it, state) } ?: Text("Pehle timeline se clip chunen")
                "Effects" -> state.selected?.let { AdjustPanel(it, state) } ?: Text("Pehle timeline se clip chunen")
                else -> Text("$tool tool agle hisson mein aayega.")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrimPanel(clip: EditClip, onTrim: (Long, Long) -> Unit) {
    if (clip.asset.isVideo) {
        var range by remember(clip.id, clip.inMs, clip.outMs) {
            mutableStateOf(clip.inMs.toFloat()..clip.outMs.toFloat())
        }
        Text("Trim: ${formatTime(range.start.toLong())} - ${formatTime(range.endInclusive.toLong())}")
        RangeSlider(
            value = range,
            onValueChange = { range = it },
            valueRange = 0f..clip.asset.durationMs.toFloat(),
            onValueChangeFinished = { onTrim(range.start.toLong(), range.endInclusive.toLong()) }
        )
    } else {
        var d by remember(clip.id, clip.durationMs) { mutableFloatStateOf(clip.durationMs.toFloat()) }
        Text("Photo ka dauraniya: " + "%.1f".format(d / 1000f) + " sec")
        Slider(
            value = d,
            onValueChange = { d = it },
            valueRange = MIN_CLIP_MS.toFloat()..PHOTO_MAX_MS.toFloat(),
            onValueChangeFinished = { onTrim(clip.inMs, clip.inMs + d.toLong()) }
        )
    }
}
