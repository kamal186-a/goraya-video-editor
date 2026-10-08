package com.goraya.videoedition.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.goraya.videoedition.edit.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioLoader {
    suspend fun load(ctx: Context, uri: Uri): Result<MusicTrack> = withContext(Dispatchers.IO) {
        runCatching {
            var duration = 0L
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(ctx, uri)
                duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                r.release()
            }
            require(duration > 0) { "Audio unsupported ya kharab hai" }
            MusicTrack(uri, displayName(ctx, uri), duration)
        }
    }

    private fun displayName(ctx: Context, uri: Uri): String = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "music"
    } catch (e: Exception) {
        "music"
    }
}
