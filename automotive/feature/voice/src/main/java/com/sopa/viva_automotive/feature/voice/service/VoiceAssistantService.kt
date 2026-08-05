package com.sopa.viva_automotive.feature.voice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sopa.viva_automotive.feature.voice.data.SpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.data.TranscriptionEvent
import com.sopa.viva_automotive.feature.voice.domain.ExecuteVehicleControlUseCase
import com.sopa.viva_automotive.feature.voice.domain.ProcessVoiceCommandUseCase
import com.sopa.viva_automotive.feature.voice.domain.VoiceAssistantStateManager
import com.sopa.viva_automotive.feature.voice.domain.VoiceTurnReport
import com.sopa.viva_automotive.feature.voice.domain.embedding.SemanticIntentMatcher
import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import com.viva.voice.trace.LatencyTrace
import com.viva.voice.trace.Stage
import com.viva.voice.trace.TraceVerdict
import com.viva.voice.trace.startSilentTrace
import com.viva.voice.trace.startVoiceTrace
import com.viva.voice.tts.TtsSpeaker
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
    @Inject lateinit var ttsSpeaker: TtsSpeaker

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
            // Đường chạy benchmark: một câu cho sẵn, không qua mic.
            //
            // Chặn ở build không debuggable. Một lối vào cho phép chạy lệnh xe
            // từ ngoài process mà không cần nói là thứ không được có mặt trong
            // bản phát hành, kể cả khi hôm nay chỉ có biến thể mock gọi tới nó.
            ACTION_SIMULATE_UTTERANCE -> {
                val debuggable =
                    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
                val utterance = intent.getStringExtra(EXTRA_UTTERANCE).orEmpty()
                when {
                    !debuggable ->
                        Log.w(VOICE_TAG, "Bỏ qua $ACTION_SIMULATE_UTTERANCE: build không debuggable")
                    utterance.isBlank() ->
                        Log.w(VOICE_TAG, "Bỏ qua $ACTION_SIMULATE_UTTERANCE: thiếu $EXTRA_UTTERANCE")
                    else -> {
                        startForegroundWithNotification()
                        warmUpEmbeddings()
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

        private fun warmUpEmbeddings() {
        if (warmUpJob?.isActive == true) return
        warmUpJob = lifecycleScope.launch {
            runCatching { semanticIntentMatcher.warmUp() }
        }
    }

    private suspend fun runInteraction() {
        speechEngine.initialize().onFailure { error ->
            stateManager.transitionToError(
                error.message ?: "Bộ nhận dạng giọng nói chưa sẵn sàng.",
            )
            delay(RESULT_DISPLAY_MS)
            stateManager.transitionToIdle()
            return
        }

        stateManager.transitionToListening()

        // One trace per turn, opened when the microphone starts. This pipeline
        // has no VAD endpointer of its own, so `speech_start` is the moment we
        // begin listening rather than a detected onset, and `speech_end` is the
        // recognizer's own endpoint decision. Both are honest for a streaming
        // on-device ASR; do not back-date them to flatter the number.
        val trace = startVoiceTrace()

        var finalText: String? = null
        var engineError: String? = null
        trace.mark(Stage.ASR_SENT)
        withTimeoutOrNull(LISTENING_TIMEOUT_MS) {
            speechEngine.transcribe().collect { event ->
                when (event) {
                    is TranscriptionEvent.Partial ->
                        stateManager.updatePartialTranscription(event.text)
                    is TranscriptionEvent.Final -> {
                        trace.mark(Stage.SPEECH_END)
                        trace.mark(Stage.ASR_DONE)
                        finalText = event.text
                    }
                    is TranscriptionEvent.Error -> engineError = event.message
                }
            }
        }

        val utterance = finalText
        when {
            engineError != null -> {
                stateManager.transitionToError(engineError)
                speak(VoiceTurnReport.DID_NOT_HEAR, trace)
                trace.summary("-", "-", TraceVerdict.Error(Stage.ASR_DONE))
            }

            utterance == null -> {
                stateManager.transitionToError(VoiceTurnReport.DID_NOT_HEAR)
                speak(VoiceTurnReport.DID_NOT_HEAR, trace)
                trace.summary("-", "-", TraceVerdict.Error(Stage.SPEECH_END))
            }

            else -> handleUtterance(trace, utterance)
        }

        delay(RESULT_DISPLAY_MS)
        stateManager.transitionToIdle()
    }

    /**
     * Everything after "we have a sentence": route it, execute it, answer, close
     * the trace.
     *
     * Tách ra khỏi [runInteraction] để [runInjectedUtterance] dùng chung **đúng
     * đường sản phẩm** — nếu chép lại logic thì benchmark sẽ đo một bản sao,
     * không đo thứ đang chạy.
     */
    private suspend fun handleUtterance(trace: LatencyTrace, utterance: String) {
        stateManager.transitionToProcessing(utterance)
        val intent = processVoiceCommand(utterance)
        trace.mark(Stage.NLU_DONE)
        if (intent is VehicleIntent.Clarification) {
            stateManager.transitionToClarification(intent.promptVi)
            finish(trace, utterance, intent, error = null)
        } else {
            stateManager.transitionToExecuting(describe(intent))
            executeVehicleControl(intent).fold(
                onSuccess = { message ->
                    trace.mark(Stage.EXEC_DONE)
                    stateManager.transitionToSuccess(message)
                    // The executor already answers in Vietnamese for the
                    // intents that have a pre-rendered clip, so the same
                    // string drives the HMI and the voice.
                    speak(message, trace)
                    trace.summary(
                        utterance,
                        VoiceTurnReport.intentName(intent),
                        TraceVerdict.Allow,
                    )
                },
                onFailure = { error ->
                    stateManager.transitionToError(error.message ?: VoiceTurnReport.COMMAND_FAILED)
                    finish(trace, utterance, intent, error)
                },
            )
        }
    }

    /**
     * Chạy một lượt với câu **cho sẵn**, bỏ qua mic và ASR.
     *
     * ## Đo được gì và KHÔNG đo được gì
     *
     * `benchmark_v1.csv` chấm `expect_intent` và `expect_verdict` — tức phần
     * router → guard → skill. Đường này chạy đúng phần đó bằng đúng mã sản
     * phẩm ([handleUtterance]). Nó **không** đo mic, VAD, ASR, WER hay độ ồn:
     * `vong2/25` §2 F7 đã ghi sẵn giới hạn này của bộ suite.
     *
     * Vì không có tiếng nói nào, lượt này **cố ý không mark** `speech_start` và
     * `speech_end`, nên `e2e_computed` để trống thay vì bịa ra một con số đẹp.
     * Ngoài ra mỗi lượt in một dòng tag [BENCH_TAG] mang cùng trace id, để bất
     * kỳ ai đọc capture về sau đều thấy ngay đây là câu bơm vào, không phải câu
     * nói. Đừng bỏ dòng đó đi.
     *
     * Chỉ chạy trên build `debuggable` — xem [onStartCommand].
     */
    private suspend fun runInjectedUtterance(utterance: String) {
        val trace = startSilentTrace()
        Log.i(BENCH_TAG, "$BENCH_TAG|${trace.traceId}|injected_text|$utterance")
        handleUtterance(trace, utterance)
        delay(RESULT_DISPLAY_MS)
        stateManager.transitionToIdle()
    }

    /** Speaks the turn's outcome and closes its trace. */
    private suspend fun finish(
        trace: LatencyTrace,
        utterance: String,
        intent: VehicleIntent,
        error: Throwable?,
    ) {
        speak(VoiceTurnReport.failureSpeech(intent, error), trace)
        trace.summary(
            utterance,
            VoiceTurnReport.intentName(intent),
            VoiceTurnReport.verdictFor(intent, error),
        )
    }

    /**
     * TTS is best-effort: it takes audio focus, and a rejected focus request or
     * a missing vi-VN voice throws. Losing the spoken answer is a degraded turn,
     * not a reason to take the assistant down mid-demo — but the failure is
     * logged so a silent run is diagnosable afterwards.
     */
    private suspend fun speak(text: String, trace: LatencyTrace) {
        runCatching { ttsSpeaker.speak(text, trace) }
            .onFailure { error -> Log.w(VOICE_TAG, "TTS failed for \"$text\": ${error.message}") }
    }

    private fun describe(intent: VehicleIntent): String = when (intent) {
        is VehicleIntent.SetTemperature -> "Đang đặt nhiệt độ"
        is VehicleIntent.AdjustTemperature -> "Đang chỉnh nhiệt độ"
        is VehicleIntent.SetFanSpeed, is VehicleIntent.AdjustFanSpeed -> "Đang chỉnh quạt"
        is VehicleIntent.SetAc -> "Đang chuyển điều hòa"
        is VehicleIntent.SetHvacPower -> "Đang chuyển hệ thống khí hậu"
        is VehicleIntent.SetDoorLock -> "Đang xử lý khóa cửa"
        is VehicleIntent.QueryStatus -> "Đang kiểm tra trạng thái xe"
        is VehicleIntent.VolumeAdjust -> "Đang chỉnh âm lượng"
        is VehicleIntent.MediaNext -> "Đang chuyển bài"
        is VehicleIntent.Delivery -> "Đang xem lộ trình giao hàng"
        is VehicleIntent.NotWired -> "Đang định tuyến lệnh"
        is VehicleIntent.Clarification -> "Đang hỏi lại"
        is VehicleIntent.Unknown -> "Đang phân tích câu lệnh"
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

        /** Diagnostics tag. Never VIVA_TRACE — that stream is the benchmark input. */
        private const val VOICE_TAG = "VIVA_VOICE"

        /**
         * Chạy một lượt với câu cho sẵn thay vì nghe mic. **Chỉ có tác dụng trên
         * build `debuggable`** — xem nhánh xử lý trong `onStartCommand`.
         */
        const val ACTION_SIMULATE_UTTERANCE =
            "com.sopa.viva_automotive.action.SIMULATE_UTTERANCE"

        /** Câu tiếng Việt cần chạy, dạng chuỗi thường (đã giải mã). */
        const val EXTRA_UTTERANCE = "utterance"

        /**
         * Tag đánh dấu lượt bơm text. Mang cùng trace id với lượt tương ứng để
         * người đọc capture về sau phân biệt được câu bơm với câu nói thật.
         */
        const val BENCH_TAG = "VIVA_BENCH_INJECT"

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
