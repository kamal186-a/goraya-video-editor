package com.goraya.videoedition.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.goraya.videoedition.edit.EditClip
import com.goraya.videoedition.edit.LookEffect
import com.goraya.videoedition.edit.lookColorMatrix
import com.goraya.videoedition.edit.lookMatrix
import com.goraya.videoedition.media.MediaLoader
import kotlinx.coroutines.delay

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** Previews the selected clip (with its trim applied). [onPosition] reports the time inside the clip. */
@Composable
fun PreviewPanel(clip: EditClip?, onPosition: (Long) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val player = remember { ExoPlayer.Builder(ctx).build() }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val dur = clip?.durationMs ?: 0L

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                error = "Yeh video play nahi ho saki (codec support nahi): ${e.errorCodeName}"
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }

    LaunchedEffect(clip?.id, clip?.inMs, clip?.outMs) {
        error = null
        pos = 0L
        onPosition(0L)
        val c = clip
        if (c != null && c.asset.isVideo) {
            val cut = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(c.inMs)
                .setEndPositionMs(c.outMs)
                .build()
            player.setMediaItem(MediaItem.Builder().setUri(c.asset.uri).setClippingConfiguration(cut).build())
            player.prepare()
            player.pause()
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!dragging) {
                pos = player.currentPosition
                onPosition(pos)
            }
            playing = player.isPlaying
            delay(200)
        }
    }

    LaunchedEffect(clip?.look) {
        val l = clip?.look
        player.setVideoEffects(if (l == null || l.isNeutral) emptyList() else listOf(LookEffect(lookMatrix(l))))
    }

    Column(modifier) {
        Box(
            Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            when {
                clip == null -> Text("Media tool se video ya photo import karen")
                clip.asset.isVideo -> AndroidView(
                    factory = { c -> PlayerView(c).apply { useController = false; this.player = player } },
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val bmp by produceState<Bitmap?>(null, clip.asset) {
                        value = MediaLoader.thumbnail(ctx, clip.asset, 1280)
                    }
                    bmp?.let {
                        val look = clip.look
                        Image(
                            it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                            colorFilter = if (look.isNeutral) null
                            else ColorFilter.colorMatrix(ColorMatrix(lookColorMatrix(lookMatrix(look))))
                        )
                    }
                }
            }
            error?.let { Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(16.dp)) }
        }
        if (clip != null && clip.asset.isVideo) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                    if (player.isPlaying) player.pause() else player.play()
                }) { Text(if (playing) "Pause" else "Play") }
                Text(formatTime(if (dragging) (dragValue * dur).toLong() else pos) + " / " + formatTime(dur))
                Slider(
                    value = if (dragging) dragValue else if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                    onValueChange = { dragging = true; dragValue = it },
                    onValueChangeFinished = {
                        val target = (dragValue * dur).toLong()
                        player.seekTo(target)
                        pos = target
                        onPosition(target)
                        dragging = false
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            }
        }
    }
}
