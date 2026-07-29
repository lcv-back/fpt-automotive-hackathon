package com.viva.voice.audio

import com.viva.voice.trace.NanoClock
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushToTalkRecorderTest {

    private class FakeClock(var now: Long = 0L) : NanoClock {
        override fun nanos(): Long = now
    }

    /** Releases the button after [chunks] polls. */
    private fun heldFor(chunks: Int): () -> Boolean {
        var polls = 0
        return { polls++ < chunks }
    }

    private fun tone(size: Int, value: Short) = ShortArray(size) { value }

    private val config = AudioConfig(
        sampleRate = 16_000,
        chunkSamples = 1_600,   // 100ms
        maxDurationMs = 1_000,  // short caps keep the tests fast and exact
        minDurationMs = 250,
    )

    @Test
    fun `holding the button captures every sample the source produced`() {
        val chunks = listOf(tone(1_600, 11), tone(1_600, 22), tone(1_600, 33))
        val source = FakePcmSource(chunks)

        val utterance = PushToTalkRecorder(source, FakeClock(), config).record(heldFor(3))

        assertEquals(4_800, utterance.pcm.size)
        assertEquals(300, utterance.durationMs)
        assertArrayEquals(chunks.flatMap { it.toList() }.toShortArray(), utterance.pcm)
    }

    @Test
    fun `the microphone is opened once and always released`() {
        val source = FakePcmSource(listOf(tone(1_600, 1)))

        PushToTalkRecorder(source, FakeClock(), config).record(heldFor(1))

        assertTrue(source.started)
        assertTrue(source.stopped)
    }

    @Test
    fun `the microphone is released even when the source throws mid-utterance`() {
        // A dead AudioRecord must not leave the mic held against the next turn.
        var stopped = false
        val exploding = object : PcmSource {
            override fun start() = Unit
            override fun read(into: ShortArray): Int = throw IllegalStateException("mic died")
            override fun stop() {
                stopped = true
            }
        }

        val thrown = runCatching {
            PushToTalkRecorder(exploding, FakeClock(), config).record(heldFor(5))
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertTrue(stopped)
    }

    @Test
    fun `a stuck button stops at the duration cap and says so`() {
        // Held far longer than maxDurationMs=1000ms: 16000 samples, no more.
        val source = FakePcmSource(emptyList(), padWith = 7)

        val utterance = PushToTalkRecorder(source, FakeClock(), config).record(heldFor(1_000))

        assertEquals(16_000, utterance.pcm.size)
        assertEquals(1_000, utterance.durationMs)
        assertTrue(utterance.truncated)
    }

    @Test
    fun `a mis-tap is flagged unusable instead of being sent to ASR`() {
        // One 100ms chunk, below minDurationMs=250ms.
        val source = FakePcmSource(listOf(tone(1_600, 5)))

        val utterance = PushToTalkRecorder(source, FakeClock(), config).record(heldFor(1))

        assertTrue(utterance.tooShort)
        assertFalse(utterance.isUsable)
    }

    @Test
    fun `a long enough press is usable`() {
        val source = FakePcmSource(List(3) { tone(1_600, 5) })

        val utterance = PushToTalkRecorder(source, FakeClock(), config).record(heldFor(3))

        assertFalse(utterance.tooShort)
        assertTrue(utterance.isUsable)
    }

    @Test
    fun `a source that ends early stops the loop without padding`() {
        val ending = object : PcmSource {
            private var reads = 0
            override fun start() = Unit
            override fun read(into: ShortArray): Int {
                reads++
                if (reads > 2) return -1
                into.fill(9)
                return into.size
            }

            override fun stop() = Unit
        }

        val utterance = PushToTalkRecorder(ending, FakeClock(), config).record(heldFor(100))

        assertEquals(3_200, utterance.pcm.size)
        assertFalse(utterance.truncated)
    }

    @Test
    fun `timestamps bracket the press and are ready for markAt`() {
        // The pipeline back-dates speech_start to startNanos, which is why
        // LatencyTrace.markAt exists - marking at hand-off time would count
        // the whole utterance as system latency.
        val clock = FakeClock(now = 5_000_000_000L)
        val source = object : PcmSource {
            override fun start() = Unit
            override fun read(into: ShortArray): Int {
                clock.now += 100_000_000L // 100ms per chunk
                into.fill(1)
                return into.size
            }

            override fun stop() = Unit
        }

        val utterance = PushToTalkRecorder(source, clock, config).record(heldFor(3))

        assertEquals(5_000_000_000L, utterance.startNanos)
        assertEquals(5_300_000_000L, utterance.endNanos)
    }
}
