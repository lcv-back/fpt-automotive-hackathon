package com.sopa.viva_automotive.feature.voice

import com.sopa.viva_automotive.feature.voice.data.vosk.VoskAsrClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRuntimeWarmUp @Inject constructor(
    private val voskAsrClient: VoskAsrClient,
) {
    suspend fun warmUp() {
        voskAsrClient.warmUp()
    }
}
