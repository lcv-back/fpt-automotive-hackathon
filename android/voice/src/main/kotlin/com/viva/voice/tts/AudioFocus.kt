package com.viva.voice.tts

import java.util.concurrent.atomic.AtomicBoolean

/** Platform-independent boundary so focus ownership can be proven on the JVM. */
interface AudioFocusController {
    fun request(onFocusLost: () -> Unit): Boolean

    fun abandon()
}

internal suspend fun <T> withAudioFocus(
    controller: AudioFocusController,
    onFocusLost: () -> Unit,
    playback: suspend (ensureFocus: () -> Unit) -> T,
): T {
    val focusLost = AtomicBoolean(false)
    val granted = controller.request {
        if (focusLost.compareAndSet(false, true)) onFocusLost()
    }
    check(granted) { "Audio focus request was rejected" }

    return try {
        val ensureFocus = {
            check(!focusLost.get()) { "Audio focus was lost during playback" }
        }
        val result = playback(ensureFocus)
        ensureFocus()
        result
    } finally {
        controller.abandon()
    }
}
