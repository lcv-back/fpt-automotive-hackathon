package com.sopa.viva_automotive.feature.voice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sopa.viva_automotive.feature.voice.domain.VoiceAssistantStateManager
import com.viva.voice.agent.VoiceAgent
import com.viva.voice.agent.VoiceTurnResult
import com.viva.voice.agent.VoiceTurnStatus
import com.viva.voice.audio.AndroidPcmSource
import com.viva.voice.audio.PushToTalkRecorder
import com.viva.voice.trace.Stage
import com.viva.voice.trace.SystemNanoClock
import com.viva.voice.trace.startSilentTrace
import com.viva.voice.trace.startVoiceTrace
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class VoiceAssistantService : LifecycleService() {

    @Inject lateinit var stateManager: VoiceAssistantStateManager
    @Inject lateinit var voiceAgent: VoiceAgent

    private var pipelineJob: Job? = null
    private val pendingTextCommands = ArrayDeque<String>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                startForegroundWithNotification()
                startPipeline()
            }
            ACTION_PROCESS_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                startForegroundWithNotification()
                startTextPipeline(text)
            }
            ACTION_STOP -> {
                pipelineJob?.cancel()
                pendingTextCommands.clear()
                stateManager.transitionToIdle()
                stopSelf()
            }
            ACTION_SIMULATE_UTTERANCE -> {
                val debuggable =
                    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                val utterance = intent.getStringExtra(EXTRA_UTTERANCE).orEmpty()
                when {
                    !debuggable ->
                        Log.w(VOICE_TAG, "Skip $ACTION_SIMULATE_UTTERANCE: non-debuggable build")
                    utterance.isBlank() ->
                        Log.w(VOICE_TAG, "Skip $ACTION_SIMULATE_UTTERANCE: missing $EXTRA_UTTERANCE")
                    else -> {
                        startForegroundWithNotification()
                        startPipeline { runInjectedUtterance(utterance) }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startPipeline(turn: suspend () -> Unit = { runInteraction() }) {
        if (pipelineJob?.isActive == true) return
        pipelineJob = lifecycleScope.launch {
            try {
                turn()
            } finally {
                stopSelf()
            }
        }
    }

    private fun startTextPipeline(text: String) {
        if (pipelineJob?.isActive == true) {
            pendingTextCommands.addLast(text)
            return
        }
        pipelineJob = lifecycleScope.launch {
            try {
                var next: String? = text
                while (next != null) {
                    val trace = startVoiceTrace()
                    trace.mark(Stage.SPEECH_END)
                    trace.mark(Stage.ASR_DONE)
                    applyAgentResult(
                        voiceAgent.handleText(next, trace),
                        displayTranscript = next,
                    )
                    next = pendingTextCommands.removeFirstOrNull()
                }
            } finally {
                stopSelf()
            }
        }
    }

    private suspend fun runInteraction() {
        stateManager.transitionToListening()

        val captured = runCatching {
            withContext(Dispatchers.IO) {
                val deadline = SystemClock.elapsedRealtime() + LISTENING_TIMEOUT_MS
                PushToTalkRecorder(AndroidPcmSource(), SystemNanoClock).record {
                    SystemClock.elapsedRealtime() < deadline
                }
            }
        }.getOrElse { error ->
            Log.e(VOICE_TAG, "Mic capture failed", error)
            stateManager.transitionToError(error.message ?: "Microphone is unavailable")
            delay(RESULT_DISPLAY_MS)
            stateManager.transitionToIdle()
            return
        }

        Log.i(
            VOICE_TAG,
            "Captured utterance samples=${captured.pcm.size} rate=${captured.sampleRate} " +
                "usable=${captured.isUsable} tooShort=${captured.tooShort}",
        )

        if (!captured.isUsable) {
            stateManager.transitionToError("I didn't hear anything")
            delay(RESULT_DISPLAY_MS)
            stateManager.transitionToIdle()
            return
        }

        val trace = startVoiceTrace(captured.startNanos)
        trace.markAt(Stage.SPEECH_END, captured.endNanos)
        stateManager.transitionToProcessing("…")
        val result = voiceAgent.handleAudio(captured.pcm, captured.sampleRate, trace)
        Log.i(
            VOICE_TAG,
            "VoiceAgent status=${result.status} transcript=\"${result.transcript}\" " +
                "spoken=\"${result.spokenVi}\"",
        )
        applyAgentResult(result, displayTranscript = result.transcript.ifBlank { "…" })
    }

    private suspend fun runInjectedUtterance(utterance: String) {
        val trace = startSilentTrace()
        Log.i(BENCH_TAG, "$BENCH_TAG|${trace.traceId}|injected_text|$utterance")
        applyAgentResult(
            voiceAgent.handleText(utterance, trace),
            displayTranscript = utterance,
        )
    }

    private suspend fun applyAgentResult(
        result: VoiceTurnResult,
        displayTranscript: String,
    ) {
        if (displayTranscript.isNotBlank() && displayTranscript != "…") {
            stateManager.transitionToProcessing(displayTranscript)
        }
        when (result.status) {
            VoiceTurnStatus.APPLIED -> stateManager.transitionToSuccess(result.spokenVi)
            VoiceTurnStatus.NEEDS_CLARIFICATION,
            VoiceTurnStatus.NEEDS_CONFIRMATION,
            -> stateManager.transitionToClarification(result.spokenVi)
            VoiceTurnStatus.DENIED,
            VoiceTurnStatus.UNSUPPORTED,
            VoiceTurnStatus.FAILED,
            -> stateManager.transitionToError(result.spokenVi)
        }
        delay(RESULT_DISPLAY_MS)
        stateManager.transitionToIdle()
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
        private const val ACTION_PROCESS_TEXT = "com.sopa.viva_automotive.action.PROCESS_TEXT"
        private const val ACTION_STOP = "com.sopa.viva_automotive.action.STOP"
        private const val EXTRA_TEXT = "text"
        private const val CHANNEL_ID = "voice_assistant"
        private const val NOTIFICATION_ID = 0x5641
        private const val LISTENING_TIMEOUT_MS = 8_000L
        private const val RESULT_DISPLAY_MS = 3_000L
        private const val VOICE_TAG = "VIVA_VOICE"

        const val ACTION_SIMULATE_UTTERANCE =
            "com.sopa.viva_automotive.action.SIMULATE_UTTERANCE"
        const val EXTRA_UTTERANCE = "utterance"
        const val BENCH_TAG = "VIVA_BENCH_INJECT"

        fun startListening(context: Context) {
            context.startForegroundService(
                Intent(context, VoiceAssistantService::class.java).setAction(ACTION_START_LISTENING),
            )
        }

        fun processText(context: Context, text: String) {
            context.startForegroundService(
                Intent(context, VoiceAssistantService::class.java)
                    .setAction(ACTION_PROCESS_TEXT)
                    .putExtra(EXTRA_TEXT, text),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, VoiceAssistantService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
