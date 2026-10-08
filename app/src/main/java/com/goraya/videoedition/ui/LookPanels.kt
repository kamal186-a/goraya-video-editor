package com.goraya.videoedition.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goraya.videoedition.edit.EditClip
import com.goraya.videoedition.edit.EditorState
import com.goraya.videoedition.edit.Filters

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    key: Any,
    onFinished: (Float) -> Unit
) {
    var v by remember(key, value) { mutableFloatStateOf(value) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label + " " + "%.2f".format(v), Modifier.width(130.dp), fontSize = 12.sp)
        Slider(
            value = v,
            onValueChange = { v = it },
            valueRange = range,
            onValueChangeFinished = { onFinished(v) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FiltersPanel(clip: EditClip, state: EditorState) {
    val look = clip.look
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        Filters.all.forEach { f ->
            TextButton(onClick = { state.setLook(look.copy(filterId = f.id)) }) {
                Text(
                    f.name,
                    color = if (f.id == look.filterId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    LabeledSlider("Shiddat", look.intensity, 0f..1f, clip.id) { state.setLook(look.copy(intensity = it)) }
    TextButton(onClick = { state.applyLookToAll(look) }) { Text("Yeh look sab clips par lagayen") }
}

@Composable
fun AdjustPanel(clip: EditClip, state: EditorState) {
    val look = clip.look
    LabeledSlider("Brightness", look.brightness, -1f..1f, clip.id) { state.setLook(look.copy(brightness = it)) }
    LabeledSlider("Contrast", look.contrast, -1f..1f, clip.id) { state.setLook(look.copy(contrast = it)) }
    LabeledSlider("Saturation", look.saturation, -1f..1f, clip.id) { state.setLook(look.copy(saturation = it)) }
    LabeledSlider("Temperature", look.temperature, -1f..1f, clip.id) { state.setLook(look.copy(temperature = it)) }
    TextButton(onClick = { state.setLook(look.copy(brightness = 0f, contrast = 0f, saturation = 0f, temperature = 0f)) }) {
        Text("Reset")
    }
}
