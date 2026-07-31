package com.viva.voice.tts

import com.viva.voice.trace.LatencyTrace

/** Implementations must mark [com.viva.voice.trace.Stage.TTS_START] when playback starts. */
interface TtsSpeaker {
    suspend fun speak(text: String, trace: LatencyTrace)

    fun stop() = Unit
}
