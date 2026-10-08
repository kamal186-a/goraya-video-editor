package com.goraya.videoedition.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaAsset(val uri: Uri, val isVideo: Boolean, val durationMs: Long, val name: String)

object MediaLoader {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(ctx: Context, uri: Uri): Result<MediaAsset> = withContext(Dispatchers.IO) {
        runCatching {
            val isVideo = (ctx.contentResolver.getType(uri) ?: "").startsWith("video")
            var duration = 0L
            if (isVideo) {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(ctx, uri)
                    duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } finally {
                    r.release()
                }
                require(duration > 0) { "Video unsupported ya kharab hai" }
            } else {
                require(decodeSampled(ctx, uri, 64) != null) { "Photo unsupported hai" }
            }
            MediaAsset(uri, isVideo, duration, displayName(ctx, uri))
        }
    }

    suspend fun thumbnail(ctx: Context, asset: MediaAsset, maxSide: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${asset.uri}@$maxSide"
        val hit = cache.get(key)
        if (hit != null) return@withContext hit
        val bmp = try {
            if (asset.isVideo) videoFrame(ctx, asset.uri, maxSide) else decodeSampled(ctx, asset.uri, maxSide)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
        if (bmp != null) cache.put(key, bmp)
        bmp
    }

    fun decodeSampled(ctx: Context, uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun videoFrame(ctx: Context, uri: Uri, maxSide: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(ctx, uri)
            val f = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            return scale(f, maxSide)
        } finally {
            r.release()
        }
    }

    private fun scale(b: Bitmap, maxSide: Int): Bitmap {
        val m = maxOf(b.width, b.height)
        if (m <= maxSide) return b
        val f = maxSide.toFloat() / m
        return Bitmap.createScaledBitmap(
            b, (b.width * f).toInt().coerceAtLeast(1), (b.height * f).toInt().coerceAtLeast(1), true
        )
    }

    private fun displayName(ctx: Context, uri: Uri): String = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "media"
    } catch (e: Exception) {
        "media"
    }
}
