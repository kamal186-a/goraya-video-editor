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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.goraya.videoedition.media.MediaAsset
import com.goraya.videoedition.media.MediaLoader
import kotlinx.coroutines.delay

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun PreviewPanel(asset: MediaAsset?, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val player = remember { ExoPlayer.Builder(ctx).build() }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

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

    LaunchedEffect(asset) {
        error = null
        pos = 0L
        if (asset != null && asset.isVideo) {
            player.setMediaItem(MediaItem.fromUri(asset.uri))
            player.prepare()
            player.pause()
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!dragging) pos = player.currentPosition
            dur = if (player.duration == C.TIME_UNSET) 0L else player.duration
            playing = player.isPlaying
            delay(200)
        }
    }

    Column(modifier) {
        Box(
            Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            when {
                asset == null -> Text("Media tool se video ya photo import karen")
                asset.isVideo -> AndroidView(
                    factory = { c -> PlayerView(c).apply { useController = false; this.player = player } },
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    val bmp by produceState<Bitmap?>(null, asset) {
                        value = MediaLoader.thumbnail(ctx, asset, 1280)
                    }
                    bmp?.let {
                        Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }
            }
            error?.let { Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(16.dp)) }
        }
        if (asset != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (asset.isVideo) {
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
                            dragging = false
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onRemove) { Text("Hatao") }
            }
        }
    }
}
