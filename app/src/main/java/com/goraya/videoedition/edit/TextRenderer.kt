package com.goraya.videoedition.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint

/**
 * Draws text layers into one transparent frame-sized bitmap. Android's StaticLayout does the
 * Arabic/Urdu shaping and right-to-left ordering. The same bitmap is used for preview and export.
 */
object TextRenderer {
    fun render(items: List<TextItem>, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        for (t in items) draw(canvas, t, bmp.width, bmp.height)
        return bmp
    }

    private fun draw(canvas: Canvas, t: TextItem, w: Int, h: Int) {
        if (t.text.isBlank()) return
        val alpha = (t.opacity.coerceIn(0f, 1f) * 255).toInt()
        val base = when (t.font) {
            1 -> Typeface.SERIF
            2 -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        val style = when {
            t.bold && t.italic -> Typeface.BOLD_ITALIC
            t.bold -> Typeface.BOLD
            t.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (t.size * h).coerceAtLeast(4f)
            color = (t.color and 0x00FFFFFF) or (alpha shl 24)
            typeface = Typeface.create(base, style)
            if (!t.background) setShadowLayer(textSize * 0.08f, 0f, 0f, 0xAA000000.toInt())
        }
        val maxW = (w * 0.9f).toInt().coerceAtLeast(1)
        val layout = StaticLayout.Builder.obtain(t.text, 0, t.text.length, paint, maxW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .build()
        var textW = 0f
        for (i in 0 until layout.lineCount) textW = maxOf(textW, layout.getLineWidth(i))

        canvas.save()
        canvas.translate(t.cx * w, t.cy * h)
        canvas.rotate(t.rotation)
        canvas.translate(-maxW / 2f, -layout.height / 2f)
        if (t.background) {
            val pad = paint.textSize * 0.3f
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ((0.55f * alpha).toInt() shl 24) }
            canvas.drawRoundRect(
                maxW / 2f - textW / 2f - pad, -pad * 0.5f,
                maxW / 2f + textW / 2f + pad, layout.height + pad * 0.5f,
                pad, pad, bg
            )
        }
        layout.draw(canvas)
        canvas.restore()
    }
}
