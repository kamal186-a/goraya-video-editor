package com.goraya.videoedition.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.goraya.videoedition.edit.EditClip
import com.goraya.videoedition.edit.LookEffect
import com.goraya.videoedition.edit.lookMatrix
import java.io.File

/** Exports the timeline to MP4 (H.264 + AAC) with Media3 Transformer and saves it to Movies/GorayaVideoEdition. */
class Exporter(private val appCtx: Context) {
    var running by mutableStateOf(false)
        private set
    var progress by mutableIntStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var transformer: Transformer? = null
    private var poll: Runnable? = null

    fun start(clips: List<EditClip>, width: Int, height: Int) {
        if (running) return
        if (clips.isEmpty()) {
            message = "Pehle timeline mein koi clip daalen"
            return
        }
        running = true
        progress = 0
        message = null
        val outFile = File(appCtx.cacheDir, "export_${System.currentTimeMillis()}.mp4")
        try {
            val items = clips.map { c ->
                val fx = mutableListOf<Effect>()
                if (!c.look.isNeutral) fx.add(LookEffect(lookMatrix(c.look)))
                fx.add(Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT))
                val effects = Effects(emptyList(), fx)
                if (c.asset.isVideo) {
                    val clip = MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(c.inMs)
                        .setEndPositionMs(c.outMs)
                        .build()
                    val mi = MediaItem.Builder().setUri(c.asset.uri).setClippingConfiguration(clip).build()
                    EditedMediaItem.Builder(mi).setEffects(effects).build()
                } else {
                    EditedMediaItem.Builder(MediaItem.fromUri(c.asset.uri))
                        .setDurationUs(c.durationMs * 1000L)
                        .setFrameRate(30)
                        .setEffects(effects)
                        .build()
                }
            }
            val composition = Composition.Builder(EditedMediaItemSequence(items)).build()
            val t = Transformer.Builder(appCtx)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        finishOk(outFile)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        fail("Export fail hua (${exportException.errorCodeName}). Is device par yeh video/codec shayad support nahi.", outFile)
                    }
                })
                .build()
            transformer = t
            t.start(composition, outFile.absolutePath)
            startPoll()
        } catch (e: Exception) {
            fail("Export shuru nahi ho saka: ${e.message}", outFile)
        } catch (e: OutOfMemoryError) {
            fail("Memory kam hai, chhoti resolution chunen", outFile)
        }
    }

    fun cancel() {
        val t = transformer ?: return
        t.cancel()
        stopPoll()
        transformer = null
        running = false
        message = "Export cancel kar diya gaya"
    }

    private fun startPoll() {
        val holder = ProgressHolder()
        val r = object : Runnable {
            override fun run() {
                val t = transformer ?: return
                if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) progress = holder.progress
                handler.postDelayed(this, 300)
            }
        }
        poll = r
        handler.post(r)
    }

    private fun stopPoll() {
        poll?.let { handler.removeCallbacks(it) }
        poll = null
    }

    private fun fail(msg: String, file: File) {
        stopPoll()
        transformer = null
        running = false
        message = msg
        file.delete()
    }

    private fun finishOk(file: File) {
        stopPoll()
        progress = 100
        Thread {
            val msg = try {
                "Export mukammal: " + saveToGallery(file)
            } catch (e: Exception) {
                "Export hua lekin save nahi ho saka: ${e.message}"
            }
            handler.post {
                transformer = null
                running = false
                message = msg
            }
        }.start()
    }

    private fun saveToGallery(file: File): String {
        val name = "Goraya_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GorayaVideoEdition")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = appCtx.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Gallery mein file nahi ban saki")
            val out = resolver.openOutputStream(uri) ?: throw IllegalStateException("Gallery file nahi khul saki")
            out.use { o -> file.inputStream().use { it.copyTo(o) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            file.delete()
            return "Movies/GorayaVideoEdition/$name"
        } else {
            val dir = appCtx.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: appCtx.filesDir
            val dest = File(dir, name)
            file.copyTo(dest, overwrite = true)
            file.delete()
            return dest.absolutePath
        }
    }
}
