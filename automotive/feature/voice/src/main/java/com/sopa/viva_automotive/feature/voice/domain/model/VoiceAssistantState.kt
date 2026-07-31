package com.sopa.viva_automotive.feature.voice.domain.model

sealed interface VoiceAssistantState {
    data object Idle : VoiceAssistantState
    data class Listening(val partialTranscription: String = "") : VoiceAssistantState
    data class Processing(val utterance: String) : VoiceAssistantState
    data class Executing(val description: String) : VoiceAssistantState
    data class Success(val message: String) : VoiceAssistantState
    data class Error(val message: String) : VoiceAssistantState
}

sealed interface VoiceEvent {
    data object ListeningStarted : VoiceEvent
    data class CommandExecuted(val message: String) : VoiceEvent
    data class CommandFailed(val message: String) : VoiceEvent
}
