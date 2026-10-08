package com.goraya.videoedition.edit

import android.net.Uri

/** Background music track mixed under the video at export. */
data class MusicTrack(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val volume: Float = 1f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L
)
