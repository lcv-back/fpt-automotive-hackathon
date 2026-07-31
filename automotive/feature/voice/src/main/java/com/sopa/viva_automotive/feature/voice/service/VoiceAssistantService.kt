package com.sopa.viva_automotive.feature.voice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sopa.viva_automotive.feature.voice.data.SpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.data.TranscriptionEvent
import com.sopa.viva_automotive.feature.voice.domain.ExecuteVehicleControlUseCase
import com.sopa.viva_automotive.feature.voice.domain.ProcessVoiceCommandUseCase
import com.sopa.viva_automotive.feature.voice.domain.VoiceAssistantStateManager
import com.sopa.viva_automotive.feature.voice.domain.embedding.SemanticIntentMatcher
import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class VoiceAssistantService : LifecycleService() {

    @Inject lateinit var speechEngine: SpeechRecognitionEngine
    @Inject lateinit var stateManager: VoiceAssistantStateManager
    @Inject lateinit var processVoiceCommand: ProcessVoiceCommandUseCase
    @Inject lateinit var executeVehicleControl: ExecuteVehicleControlUseCase
    @Inject lateinit var semanticIntentMatcher: SemanticIntentMatcher

    private var pipelineJob: Job? = null
    private var warmUpJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                startForegroundWithNotification()
                warmUpEmbeddings()
                startPipeline()
            }
            ACTION_STOP -> {
                pipelineJob?.cancel()
                stateManager.transitionToIdle()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startPipeline() {
        if (pipelineJob?.isActive == true) return
        pipelineJob = lifecycleScope.launch {
            try {
                runInteraction()
            } finally {
                stopSelf()
            }
        }
    }

        private fun warmUpEmbeddings() {
        if (warmUpJob?.isActive == true) return
        warmUpJob = lifecycleScope.launch {
            runCatching { semanticIntentMatcher.warmUp() }
        }
    }

    private suspend fun runInteraction() {
        speechEngine.initialize().onFailure { error ->
            stateManager.transitionToError(
                error.message ?: "Voice recognition is unavailable",
            )
            delay(RESULT_DISPLAY_MS)
            stateManager.transitionToIdle()
            return
        }

        stateManager.transitionToListening()

        var finalText: String? = null
        var engineError: String? = null
        withTimeoutOrNull(LISTENING_TIMEOUT_MS) {
            speechEngine.transcribe().collect { event ->
                when (event) {
                    is TranscriptionEvent.Partial ->
                        stateManager.updatePartialTranscription(event.text)
                    is TranscriptionEvent.Final -> finalText = event.text
                    is TranscriptionEvent.Error -> engineError = event.message
                }
            }
        }

        val utterance = finalText
        when {
            engineError != null -> stateManager.transitionToError(engineError)
            utterance == null -> stateManager.transitionToError("I didn't hear anything")
            else -> {
                stateManager.transitionToProcessing(utterance)
                val intent = processVoiceCommand(utterance)
                if (intent is VehicleIntent.Clarification) {
                    stateManager.transitionToClarification(intent.promptVi)
                } else {
                    stateManager.transitionToExecuting(describe(intent))
                    executeVehicleControl(intent).fold(
                        onSuccess = { message -> stateManager.transitionToSuccess(message) },
                        onFailure = { error ->
                            stateManager.transitionToError(error.message ?: "Command failed")
                        },
                    )
                }
            }
        }

        delay(RESULT_DISPLAY_MS)
        stateManager.transitionToIdle()
    }

    private fun describe(intent: VehicleIntent): String = when (intent) {
        is VehicleIntent.SetTemperature -> "Setting temperature"
        is VehicleIntent.AdjustTemperature -> "Adjusting temperature"
        is VehicleIntent.SetFanSpeed, is VehicleIntent.AdjustFanSpeed -> "Adjusting fan"
        is VehicleIntent.SetAc -> "Switching air conditioning"
        is VehicleIntent.SetHvacPower -> "Switching climate system"
        is VehicleIntent.SetDoorLock -> "Updating door locks"
        is VehicleIntent.QueryStatus -> "Checking vehicle status"
        is VehicleIntent.Clarification -> "Clarifying command"
        is VehicleIntent.Unknown -> "Interpreting command"
    }

    private fun startForegroundWithNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Voice assistant",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Viva voice assistant")
            .setContentText("Listening for a command")
            .setOngoing(true)
            .build()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
    }

    companion object {
        private const val ACTION_START_LISTENING = "com.sopa.viva_automotive.action.START_LISTENING"
        private const val ACTION_STOP = "com.sopa.viva_automotive.action.STOP"
        private const val CHANNEL_ID = "voice_assistant"
        private const val NOTIFICATION_ID = 0x5641
        private const val LISTENING_TIMEOUT_MS = 15_000L
        private const val RESULT_DISPLAY_MS = 3_000L

        fun startListening(context: Context) {
            context.startForegroundService(
                Intent(context, VoiceAssistantService::class.java).setAction(ACTION_START_LISTENING),
            )
        }

                fun stop(context: Context) {
            context.startService(
                Intent(context, VoiceAssistantService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
