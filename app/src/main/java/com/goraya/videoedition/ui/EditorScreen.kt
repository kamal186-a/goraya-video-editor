package com.goraya.videoedition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goraya.videoedition.engine.VideoEngine

private val tools = listOf("Media", "Audio", "Text", "Effects", "Filters", "Stickers", "Canvas", "Speed", "Export")

@Composable
fun EditorScreen() {
    var selected by remember { mutableStateOf(tools.first()) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Text("Goraya Video Edition", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) { Text("Preview (Phase 2)") }
        Box(
            Modifier.height(120.dp).fillMaxWidth().padding(16.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) { Text("Timeline (Phase 2) — effects loaded: ${VideoEngine.availableEffectCount()}") }
        Text("Tool: $selected", Modifier.padding(horizontal = 16.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
            tools.forEach { t ->
                TextButton(onClick = { selected = t }) { Text(t) }
            }
        }
    }
}
