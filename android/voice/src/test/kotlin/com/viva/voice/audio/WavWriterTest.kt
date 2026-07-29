package com.viva.voice.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WavWriterTest {

    private fun ascii(wav: ByteArray, at: Int, len: Int) =
        String(wav, at, len, Charsets.US_ASCII)

    private fun le32(wav: ByteArray, at: Int): Int =
        (wav[at].toInt() and 0xFF) or
            ((wav[at + 1].toInt() and 0xFF) shl 8) or
            ((wav[at + 2].toInt() and 0xFF) shl 16) or
            ((wav[at + 3].toInt() and 0xFF) shl 24)

    private fun le16(wav: ByteArray, at: Int): Int =
        (wav[at].toInt() and 0xFF) or ((wav[at + 1].toInt() and 0xFF) shl 8)

    @Test
    fun `header declares canonical mono PCM16`() {
        val wav = WavWriter.toWav(ShortArray(160), sampleRate = 16_000)

        assertEquals("RIFF", ascii(wav, 0, 4))
        assertEquals("WAVE", ascii(wav, 8, 4))
        assertEquals("fmt ", ascii(wav, 12, 4))
        assertEquals(16, le32(wav, 16))          // PCM fmt chunk size
        assertEquals(1, le16(wav, 20))           // format = PCM
        assertEquals(1, le16(wav, 22))           // channels = mono
        assertEquals(16_000, le32(wav, 24))      // sample rate
        assertEquals(32_000, le32(wav, 28))      // byte rate = 16000 * 1 * 2
        assertEquals(2, le16(wav, 32))           // block align
        assertEquals(16, le16(wav, 34))          // bits per sample
        assertEquals("data", ascii(wav, 36, 4))
    }

    @Test
    fun `the two size fields agree with the actual byte count`() {
        // A player trusts these fields; if they disagree with the file the
        // clip plays as silence or as garbage, which is indistinguishable
        // from a broken microphone when debugging L3a.
        val wav = WavWriter.toWav(ShortArray(1_600), sampleRate = 16_000)

        assertEquals(WavWriter.HEADER_BYTES + 3_200, wav.size)
        assertEquals(36 + 3_200, le32(wav, 4))
        assertEquals(3_200, le32(wav, 40))
    }

    @Test
    fun `samples are written little-endian, including negative ones`() {
        val wav = WavWriter.toWav(shortArrayOf(0x0102, -2, Short.MIN_VALUE), 16_000)
        val body = wav.copyOfRange(WavWriter.HEADER_BYTES, wav.size)

        assertEquals(0x02.toByte(), body[0])
        assertEquals(0x01.toByte(), body[1])
        assertEquals(0xFE.toByte(), body[2])
        assertEquals(0xFF.toByte(), body[3])
        assertEquals(0x00.toByte(), body[4])
        assertEquals(0x80.toByte(), body[5])
    }

    @Test
    fun `the body after the header is exactly the payload the ASR endpoint wants`() {
        // 03-contracts.md §2 posts "raw PCM 16-bit LE mono" with no header,
        // so the same buffer must serve both by dropping 44 bytes.
        val pcm = ShortArray(320) { (it * 7).toShort() }

        val body = WavWriter.toWav(pcm, 16_000).copyOfRange(WavWriter.HEADER_BYTES, 44 + 640)

        assertEquals(pcm.size * 2, body.size)
        assertEquals((pcm[5].toInt() and 0xFF).toByte(), body[10])
    }

    @Test
    fun `an empty capture still produces a valid, zero-length file`() {
        val wav = WavWriter.toWav(ShortArray(0), 16_000)

        assertEquals(WavWriter.HEADER_BYTES, wav.size)
        assertEquals(0, le32(wav, 40))
    }

    @Test
    fun `a nonsense sample rate is rejected at the boundary`() {
        assertThrows(IllegalArgumentException::class.java) {
            WavWriter.toWav(ShortArray(16), sampleRate = 0)
        }
    }
}
