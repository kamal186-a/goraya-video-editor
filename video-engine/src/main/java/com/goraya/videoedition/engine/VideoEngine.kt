package com.goraya.videoedition.engine

import com.goraya.videoedition.effects.EffectRegistry
import com.goraya.videoedition.timeline.Timeline

/** Entry point of the engine. Preview (Media3) and export arrive in Phase 2. */
object VideoEngine {
    fun availableEffectCount(): Int = EffectRegistry.all().size
    fun emptyProject(): Timeline = Timeline()
}
