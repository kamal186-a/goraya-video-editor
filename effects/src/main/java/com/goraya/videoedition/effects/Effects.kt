package com.goraya.videoedition.effects

/** A GPU effect described by a GLSL ES fragment shader and named float parameters. */
data class EffectPreset(
    val id: String,
    val displayName: String,
    val fragmentShader: String,
    val defaults: Map<String, Float> = emptyMap()
)

/** Central registry: adding a new effect = registering one more EffectPreset. */
object EffectRegistry {
    private val items = linkedMapOf<String, EffectPreset>()
    fun register(p: EffectPreset) { items[p.id] = p }
    fun all(): List<EffectPreset> = items.values.toList()
    fun get(id: String): EffectPreset? = items[id]

    init {
        register(EffectPreset("brightness", "Brightness", BRIGHTNESS, mapOf("amount" to 0f)))
    }

    private const val BRIGHTNESS = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform float amount;
        void main() {
            vec4 c = texture2D(uTexture, vTexCoord);
            gl_FragColor = vec4(c.rgb + amount, c.a);
        }
    """
}
