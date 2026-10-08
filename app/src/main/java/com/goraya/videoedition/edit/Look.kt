package com.goraya.videoedition.edit

import androidx.media3.effect.RgbMatrix

/** Colour look of a clip: a preset filter (scaled by intensity) plus manual adjustments. All values -1..1. */
data class Look(
    val filterId: String = "none",
    val intensity: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f
) {
    val isNeutral: Boolean
        get() = filterId == "none" && brightness == 0f && contrast == 0f && saturation == 0f && temperature == 0f
}

data class FilterPreset(
    val id: String,
    val name: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float
)

/** Original colour presets. To add a filter, add one more FilterPreset here. */
object Filters {
    val all = listOf(
        FilterPreset("none", "Original", 0f, 0f, 0f, 0f),
        FilterPreset("cinematic", "Cinematic", -0.03f, 0.25f, -0.15f, 0.10f),
        FilterPreset("warm", "Warm", 0f, 0f, 0.10f, 0.55f),
        FilterPreset("cool", "Cool", 0f, 0f, 0.05f, -0.55f),
        FilterPreset("vintage", "Vintage", 0.05f, -0.10f, -0.35f, 0.30f),
        FilterPreset("bw", "B&W", 0f, 0.10f, -1f, 0f),
        FilterPreset("contrast", "High Contrast", 0f, 0.55f, 0.15f, 0f),
        FilterPreset("soft", "Soft", 0.05f, -0.20f, -0.05f, 0f),
        FilterPreset("bright", "Bright", 0.20f, 0.05f, 0.05f, 0f),
        FilterPreset("dark", "Dark", -0.25f, 0.10f, 0f, 0f),
        FilterPreset("film", "Film", 0.02f, 0.15f, -0.20f, 0.15f)
    )

    fun byId(id: String): FilterPreset = all.firstOrNull { it.id == id } ?: all[0]
}

private fun identity(): FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f }

/** a * b for 4x4 column-major matrices (b is applied first). */
private fun mul(a: FloatArray, b: FloatArray): FloatArray {
    val r = FloatArray(16)
    for (col in 0..3) for (row in 0..3) {
        var s = 0f
        for (k in 0..3) s += a[k * 4 + row] * b[col * 4 + k]
        r[col * 4 + row] = s
    }
    return r
}

/** 4x4 column-major RGBA matrix (bias in the 4th column) for the given look. */
fun lookMatrix(l: Look): FloatArray {
    val p = Filters.byId(l.filterId)
    val k = l.intensity
    val b = (p.brightness * k + l.brightness).coerceIn(-1f, 1f)
    val c = (p.contrast * k + l.contrast).coerceIn(-1f, 1f)
    val s = (p.saturation * k + l.saturation).coerceIn(-1f, 1f)
    val t = (p.temperature * k + l.temperature).coerceIn(-1f, 1f)

    val temp = identity().also { it[0] = 1f + 0.2f * t; it[10] = 1f - 0.2f * t }

    val f = 1f + s
    val lum = floatArrayOf(0.2126f, 0.7152f, 0.0722f)
    val sat = identity()
    for (row in 0..2) for (col in 0..2) {
        sat[col * 4 + row] = lum[col] * (1f - f) + (if (row == col) f else 0f)
    }

    val kc = 1f + c
    val contrast = identity().also {
        it[0] = kc; it[5] = kc; it[10] = kc
        val bias = 0.5f * (1f - kc)
        it[12] = bias; it[13] = bias; it[14] = bias
    }

    val bright = identity().also { val v = b * 0.4f; it[12] = v; it[13] = v; it[14] = v }

    return mul(bright, mul(contrast, mul(sat, temp)))
}

/** Converts [lookMatrix] to the 4x5 row-major layout used by android/compose ColorMatrix (offsets in 0..255). */
fun lookColorMatrix(m: FloatArray): FloatArray {
    val out = FloatArray(20)
    for (i in 0..3) {
        for (j in 0..3) out[i * 5 + j] = m[j * 4 + i]
        out[i * 5 + 4] = if (i < 3) m[12 + i] * 255f else 0f
    }
    return out
}

/** GPU effect (Media3) that applies a colour matrix. Works in both preview and export. */
class LookEffect(private val matrix: FloatArray) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
}
