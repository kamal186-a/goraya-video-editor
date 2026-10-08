package com.goraya.videoedition.export

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/** Applies a constant gain plus linear fade-in / fade-out to 16-bit PCM audio. */
class GainFadeAudioProcessor(
    private val gain: Float,
    private val fadeInUs: Long,
    private val fadeOutUs: Long,
    private val totalUs: Long
) : BaseAudioProcessor() {
    private var frames = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        frames = 0L
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val out = replaceOutputBuffer(remaining)
        val channels = inputAudioFormat.channelCount
        val rate = inputAudioFormat.sampleRate
        while (inputBuffer.remaining() >= 2 * channels) {
            val tUs = frames * 1_000_000L / rate
            var g = gain
            if (fadeInUs > 0 && tUs < fadeInUs) g *= tUs.toFloat() / fadeInUs
            val left = totalUs - tUs
            if (fadeOutUs > 0 && left < fadeOutUs) g *= left.coerceAtLeast(0L).toFloat() / fadeOutUs
            for (c in 0 until channels) {
                val s = (inputBuffer.short * g).toInt().coerceIn(-32768, 32767)
                out.putShort(s.toShort())
            }
            frames++
        }
        out.flip()
    }
}
