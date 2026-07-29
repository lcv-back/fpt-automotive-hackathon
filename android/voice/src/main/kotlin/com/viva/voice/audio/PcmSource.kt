package com.viva.voice.audio

/**
 * Audio capture settings for the whole voice pipeline.
 *
 * 16 kHz mono PCM16 is not a preference - it is what the ASR container
 * expects (03-contracts.md §2: `X-Sample-Rate: 16000`, "raw PCM 16-bit LE
 * mono") and what Silero VAD is trained on (L3b). Resampling later would
 * cost latency we have budgeted at 1500ms end to end.
 */
data class AudioConfig(
    val sampleRate: Int = 16_000,
    /** 100ms per read. Small enough to release the button responsively. */
    val chunkSamples: Int = 1_600,
    /**
     * A stuck button must not stream forever: 15s of 16kHz PCM16 is 480KB,
     * anything past that is a bug, not a command.
     */
    val maxDurationMs: Int = 15_000,
    /**
     * Below this a "press" is a mis-tap. Sending it to ASR wastes a round
     * trip and comes back as noise the intent router then has to reject.
     */
    val minDurationMs: Int = 250,
) {
    val bytesPerSample: Int get() = 2

    companion object {
        val DEFAULT = AudioConfig()
    }
}

/**
 * A stream of 16-bit PCM samples.
 *
 * Exists so [PushToTalkRecorder] never touches `android.media.AudioRecord`:
 * the recorder's buffering, duration limits and truncation rules are unit
 * tested on a plain JVM against [FakePcmSource]. The real implementation is
 * [AndroidPcmSource].
 */
interface PcmSource {
    /** Opens the microphone. Throws if the device refuses to record. */
    fun start()

    /**
     * Fills [into] with up to `into.size` samples, returning how many were
     * written, or -1 when the source has ended. Blocks until samples are
     * available, like AudioRecord.read does.
     */
    fun read(into: ShortArray): Int

    /** Releases the microphone. Safe to call twice. */
    fun stop()
}

/** Deterministic [PcmSource] for tests and for the sound-check fixture. */
class FakePcmSource(
    private val chunks: List<ShortArray>,
    /** Emitted forever once [chunks] runs out, so "held" tests can keep reading. */
    private val padWith: Short = 0,
) : PcmSource {
    var started = false
        private set
    var stopped = false
        private set
    private var index = 0

    override fun start() {
        started = true
    }

    override fun read(into: ShortArray): Int {
        val chunk = chunks.getOrNull(index)
        index++
        if (chunk == null) {
            into.fill(padWith)
            return into.size
        }
        val n = minOf(chunk.size, into.size)
        System.arraycopy(chunk, 0, into, 0, n)
        return n
    }

    override fun stop() {
        stopped = true
    }
}
