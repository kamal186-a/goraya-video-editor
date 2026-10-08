package com.goraya.videoedition.edit

/** A text layer on a clip. Positions are fractions of the output frame (0..1); size is a fraction of frame height. */
data class TextItem(
    val id: Long,
    val text: String = "Text",
    val size: Float = 0.07f,
    val color: Int = 0xFFFFFFFF.toInt(),
    val bold: Boolean = true,
    val italic: Boolean = false,
    val background: Boolean = false,
    val font: Int = 0,            // 0 sans, 1 serif, 2 mono
    val cx: Float = 0.5f,
    val cy: Float = 0.5f,
    val opacity: Float = 1f,
    val rotation: Float = 0f
)

data class CanvasOption(val label: String, val aw: Int, val ah: Int)

val CANVAS_OPTIONS = listOf(
    CanvasOption("16:9", 16, 9),
    CanvasOption("9:16", 9, 16),
    CanvasOption("1:1", 1, 1),
    CanvasOption("4:5", 4, 5),
    CanvasOption("3:4", 3, 4)
)
