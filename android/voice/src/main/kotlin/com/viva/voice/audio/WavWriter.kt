package com.viva.voice.audio

/**
 * Wraps raw PCM16 in a 44-byte canonical WAV header.
 *
 * Needed for two things, neither of them the ASR call (03-contracts.md §2
 * posts headerless PCM):
 *
 *  1. L3a's definition of done - "giu nut -> ra file wav nghe ro". A file you
 *     can play is the only way to tell "the mic is muted" apart from "the VAD
 *     is cutting too early" before Silero even exists.
 *  2. V12's 20 utterances x 3 noise levels: the corpus has to be replayable,
 *     and 03-contracts.md §10 already expects synthetic input for the harness.
 *
 * Pure Kotlin on purpose - byte-level format code is exactly what unit tests
 * are good at, and none of it needs a device.
 */
object WavWriter {

    const val HEADER_BYTES = 44
    private const val PCM_FORMAT = 1
    private const val BITS_PER_SAMPLE = 16

    /**
     * Returns a complete mono 16-bit WAV file.
     *
     * Little-endian throughout, which the format mandates and which also
     * matches the "raw PCM 16-bit LE mono" the ASR container expects, so the
     * body can be sent as-is by stripping the first [HEADER_BYTES].
     */
    fun toWav(pcm: ShortArray, sampleRate: Int, channels: Int = 1): ByteArray {
        require(sampleRate > 0) { "sampleRate must be positive, got $sampleRate" }
        require(channels > 0) { "channels must be positive, got $channels" }

        val dataBytes = pcm.size * 2
        val out = ByteArray(HEADER_BYTES + dataBytes)
        val byteRate = sampleRate * channels * BITS_PER_SAMPLE / 8
        val blockAlign = channels * BITS_PER_SAMPLE / 8

        ascii(out, 0, "RIFF")
        le32(out, 4, 36 + dataBytes)          // size of everything after this field
        ascii(out, 8, "WAVE")
        ascii(out, 12, "fmt ")
        le32(out, 16, 16)                     // fmt chunk size for PCM
        le16(out, 20, PCM_FORMAT)
        le16(out, 22, channels)
        le32(out, 24, sampleRate)
        le32(out, 28, byteRate)
        le16(out, 32, blockAlign)
        le16(out, 34, BITS_PER_SAMPLE)
        ascii(out, 36, "data")
        le32(out, 40, dataBytes)

        var at = HEADER_BYTES
        for (sample in pcm) {
            out[at++] = (sample.toInt() and 0xFF).toByte()
            out[at++] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun ascii(out: ByteArray, at: Int, s: String) {
        for (i in s.indices) out[at + i] = s[i].code.toByte()
    }

    private fun le16(out: ByteArray, at: Int, v: Int) {
        out[at] = (v and 0xFF).toByte()
        out[at + 1] = ((v shr 8) and 0xFF).toByte()
    }

    private fun le32(out: ByteArray, at: Int, v: Int) {
        out[at] = (v and 0xFF).toByte()
        out[at + 1] = ((v shr 8) and 0xFF).toByte()
        out[at + 2] = ((v shr 16) and 0xFF).toByte()
        out[at + 3] = ((v shr 24) and 0xFF).toByte()
    }
}
