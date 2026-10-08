package com.goraya.videoedition.project

import android.content.Context
import android.net.Uri
import com.goraya.videoedition.edit.CANVAS_OPTIONS
import com.goraya.videoedition.edit.EditClip
import com.goraya.videoedition.edit.EditorState
import com.goraya.videoedition.edit.Look
import com.goraya.videoedition.edit.MusicTrack
import com.goraya.videoedition.edit.TextItem
import com.goraya.videoedition.media.MediaAsset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Saves/loads the whole project (clips, trims, looks, text, music, canvas) as JSON in app storage. */
object ProjectStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "project.json")

    fun save(ctx: Context, s: EditorState): Boolean = try {
        val root = JSONObject()
        root.put("version", 1)
        root.put("canvas", s.canvas.label)
        val arr = JSONArray()
        for (c in s.clips) arr.put(clipJson(c))
        root.put("clips", arr)
        s.music?.let { m ->
            root.put(
                "music",
                JSONObject()
                    .put("uri", m.uri.toString())
                    .put("name", m.name)
                    .put("durationMs", m.durationMs)
                    .put("volume", m.volume.toDouble())
                    .put("fadeInMs", m.fadeInMs)
                    .put("fadeOutMs", m.fadeOutMs)
            )
        }
        val tmp = File(ctx.filesDir, "project.tmp")
        tmp.writeText(root.toString())
        if (!tmp.renameTo(file(ctx))) {
            file(ctx).writeText(root.toString())
            tmp.delete()
        }
        true
    } catch (e: Exception) {
        false
    }

    /** Returns a message to show, or null when there is nothing to report. */
    fun load(ctx: Context, s: EditorState): String? {
        val f = file(ctx)
        if (!f.exists()) return null
        return try {
            val root = JSONObject(f.readText())
            val canvas = CANVAS_OPTIONS.firstOrNull { it.label == root.optString("canvas") } ?: CANVAS_OPTIONS[0]
            val arr = root.optJSONArray("clips") ?: JSONArray()
            val list = mutableListOf<EditClip>()
            var missing = 0
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val uri = Uri.parse(o.getString("uri"))
                if (!accessible(ctx, uri)) { missing++; continue }
                list.add(clipFrom(o, uri))
            }
            var music: MusicTrack? = null
            val mo = root.optJSONObject("music")
            if (mo != null) {
                val u = Uri.parse(mo.getString("uri"))
                if (accessible(ctx, u)) {
                    music = MusicTrack(
                        u, mo.optString("name", "music"), mo.optLong("durationMs"),
                        mo.optDouble("volume", 1.0).toFloat(), mo.optLong("fadeInMs"), mo.optLong("fadeOutMs")
                    )
                } else missing++
            }
            s.restore(list, music, canvas)
            when {
                missing > 0 -> "$missing file ab nahi mil rahi, unhein hata diya gaya"
                list.isNotEmpty() -> "Pichla project khul gaya"
                else -> null
            }
        } catch (e: Exception) {
            "Purana project load nahi ho saka"
        }
    }

    private fun accessible(ctx: Context, uri: Uri): Boolean = try {
        val st = ctx.contentResolver.openInputStream(uri)
        if (st == null) false else { st.close(); true }
    } catch (e: Exception) {
        false
    }

    private fun clipJson(c: EditClip): JSONObject {
        val texts = JSONArray()
        for (t in c.texts) {
            texts.put(
                JSONObject()
                    .put("id", t.id).put("text", t.text).put("size", t.size.toDouble()).put("color", t.color)
                    .put("bold", t.bold).put("italic", t.italic).put("background", t.background)
                    .put("font", t.font).put("cx", t.cx.toDouble()).put("cy", t.cy.toDouble())
                    .put("opacity", t.opacity.toDouble()).put("rotation", t.rotation.toDouble())
            )
        }
        val l = c.look
        return JSONObject()
            .put("id", c.id).put("uri", c.asset.uri.toString()).put("isVideo", c.asset.isVideo)
            .put("durationMs", c.asset.durationMs).put("name", c.asset.name)
            .put("inMs", c.inMs).put("outMs", c.outMs).put("muted", c.muted)
            .put(
                "look",
                JSONObject().put("filterId", l.filterId).put("intensity", l.intensity.toDouble())
                    .put("brightness", l.brightness.toDouble()).put("contrast", l.contrast.toDouble())
                    .put("saturation", l.saturation.toDouble()).put("temperature", l.temperature.toDouble())
            )
            .put("texts", texts)
    }

    private fun clipFrom(o: JSONObject, uri: Uri): EditClip {
        val asset = MediaAsset(uri, o.getBoolean("isVideo"), o.optLong("durationMs"), o.optString("name", "media"))
        val lo = o.optJSONObject("look")
        val look = if (lo == null) Look() else Look(
            lo.optString("filterId", "none"), lo.optDouble("intensity", 1.0).toFloat(),
            lo.optDouble("brightness", 0.0).toFloat(), lo.optDouble("contrast", 0.0).toFloat(),
            lo.optDouble("saturation", 0.0).toFloat(), lo.optDouble("temperature", 0.0).toFloat()
        )
        val texts = mutableListOf<TextItem>()
        val ta = o.optJSONArray("texts")
        if (ta != null) for (i in 0 until ta.length()) {
            val t = ta.getJSONObject(i)
            texts.add(
                TextItem(
                    id = t.getLong("id"), text = t.optString("text", "Text"),
                    size = t.optDouble("size", 0.07).toFloat(), color = t.optInt("color", -1),
                    bold = t.optBoolean("bold", true), italic = t.optBoolean("italic", false),
                    background = t.optBoolean("background", false), font = t.optInt("font", 0),
                    cx = t.optDouble("cx", 0.5).toFloat(), cy = t.optDouble("cy", 0.5).toFloat(),
                    opacity = t.optDouble("opacity", 1.0).toFloat(), rotation = t.optDouble("rotation", 0.0).toFloat()
                )
            )
        }
        return EditClip(
            id = o.getLong("id"), asset = asset, inMs = o.getLong("inMs"), outMs = o.getLong("outMs"),
            look = look, texts = texts, muted = o.optBoolean("muted", false)
        )
    }
}
