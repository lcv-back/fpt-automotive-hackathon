package com.viva.voice.asr

import com.viva.voice.trace.LatencyTrace
import com.viva.voice.trace.Stage

interface AsrClient {
    suspend fun transcribe(
        pcm16: ShortArray,
        sampleRate: Int,
        trace: LatencyTrace,
    ): AsrResult
}

data class AsrResult(
    val text: String,
    val confidence: Float,
    val serverMs: Int,
    val isPartial: Boolean = false,
) {
    init {
        require(confidence in 0f..1f) { "confidence must be between 0 and 1" }
        require(serverMs >= 0) { "serverMs must not be negative" }
    }
}

/** Immediate ASR fake for UI integration and deterministic tests. */
class FakeAsrClient(
    private val result: AsrResult = AsrResult("", 1f, 0),
) : AsrClient {
    override suspend fun transcribe(
        pcm16: ShortArray,
        sampleRate: Int,
        trace: LatencyTrace,
    ): AsrResult {
        require(pcm16.isNotEmpty()) { "pcm16 must not be empty" }
        require(sampleRate > 0) { "sampleRate must be positive" }
        trace.mark(Stage.ASR_SENT)
        trace.mark(Stage.ASR_DONE)
        return result
    }
}
