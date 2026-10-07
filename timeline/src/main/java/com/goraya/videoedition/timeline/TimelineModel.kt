package com.goraya.videoedition.timeline

enum class TrackType { VIDEO, AUDIO, TEXT, STICKER, OVERLAY, IMAGE, EFFECT }
enum class Easing { LINEAR, EASE_IN, EASE_OUT }

data class Keyframe(val timeMs: Long, val value: Float, val easing: Easing = Easing.LINEAR)

data class Clip(
    val id: String,
    val sourceUri: String?,
    val startMs: Long,          // position on the timeline
    val durationMs: Long,
    val trimInMs: Long = 0,     // offset inside the source media
    val speed: Float = 1f,
    val visible: Boolean = true,
    val keyframes: Map<String, List<Keyframe>> = emptyMap() // property name -> keyframes
) {
    val endMs: Long get() = startMs + durationMs
}

data class Track(val id: String, val type: TrackType, val clips: List<Clip> = emptyList())

data class Timeline(val tracks: List<Track> = emptyList()) {
    val durationMs: Long get() = tracks.maxOfOrNull { t -> t.clips.maxOfOrNull { it.endMs } ?: 0L } ?: 0L
}
