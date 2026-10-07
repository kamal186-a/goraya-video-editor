package com.goraya.videoedition.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF17171A),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun GorayaTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Dark, content = content)
