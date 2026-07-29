package com.sopa.viva_automotive.feature.voice.data

import kotlinx.coroutines.flow.Flow

sealed interface TranscriptionEvent {
    data class Partial(val text: String) : TranscriptionEvent
    data class Final(val text: String) : TranscriptionEvent
    data class Error(val message: String, val cause: Throwable? = null) : TranscriptionEvent
}

interface SpeechRecognitionEngine {
    suspend fun initialize(): Result<Unit>
    fun transcribe(): Flow<TranscriptionEvent>
    fun requestEndOfUtterance() {}
}
