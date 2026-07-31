package com.viva.voice.tts

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioFocusTest {

    private class FakeAudioFocusController(
        private val grant: Boolean,
    ) : AudioFocusController {
        private var lossCallback: (() -> Unit)? = null
        var abandonCount = 0

        override fun request(onFocusLost: () -> Unit): Boolean {
            lossCallback = onFocusLost
            return grant
        }

        override fun abandon() {
            abandonCount += 1
            lossCallback = null
        }

        fun loseFocus() {
            lossCallback?.invoke()
        }
    }

    @Test
    fun `playback does not start when transient focus is rejected`() {
        val focus = FakeAudioFocusController(grant = false)
        var played = false

        assertThrows(IllegalStateException::class.java) {
            runImmediate {
                withAudioFocus(focus, onFocusLost = {}) {
                    played = true
                }
            }
        }

        assertEquals(false, played)
        assertEquals(0, focus.abandonCount)
    }

    @Test
    fun `focus is abandoned after successful playback`() {
        val focus = FakeAudioFocusController(grant = true)

        runImmediate {
            withAudioFocus(focus, onFocusLost = {}) { "spoken" }
        }

        assertEquals(1, focus.abandonCount)
    }

    @Test
    fun `focus is abandoned when playback fails`() {
        val focus = FakeAudioFocusController(grant = true)

        assertThrows(IllegalArgumentException::class.java) {
            runImmediate {
                withAudioFocus(focus, onFocusLost = {}) {
                    throw IllegalArgumentException("TTS engine failed")
                }
            }
        }

        assertEquals(1, focus.abandonCount)
    }

    @Test
    fun `losing focus stops playback and fails the utterance`() {
        val focus = FakeAudioFocusController(grant = true)
        var stopCount = 0

        assertThrows(IllegalStateException::class.java) {
            runImmediate {
                withAudioFocus(focus, onFocusLost = { stopCount += 1 }) {
                    focus.loseFocus()
                }
            }
        }

        assertEquals(1, stopCount)
        assertEquals(1, focus.abandonCount)
    }

    @Test
    fun `focus loss prevents a fallback playback attempt`() {
        val focus = FakeAudioFocusController(grant = true)
        var fallbackPlayed = false

        assertThrows(IllegalStateException::class.java) {
            runImmediate {
                withAudioFocus(focus, onFocusLost = {}) { ensureFocus ->
                    focus.loseFocus()
                    ensureFocus()
                    fallbackPlayed = true
                }
            }
        }

        assertEquals(false, fallbackPlayed)
        assertEquals(1, focus.abandonCount)
    }

    private fun <T> runImmediate(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        })
        return outcome?.getOrThrow()
            ?: error("Test fake suspended; runImmediate only supports immediate fakes")
    }
}
