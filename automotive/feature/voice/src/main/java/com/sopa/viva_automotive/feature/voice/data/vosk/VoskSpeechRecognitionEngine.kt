package com.sopa.viva_automotive.feature.voice.data.vosk

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.sopa.viva_automotive.core.common.coroutines.IoDispatcher
import com.sopa.viva_automotive.core.database.settings.SettingsDataStore
import com.sopa.viva_automotive.core.ui.locale.VoiceLanguage
import com.sopa.viva_automotive.feature.voice.data.SpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.data.TranscriptionEvent
import com.viva.voice.audio.PcmFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

@Singleton
class VoskSpeechRecognitionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val settingsDataStore: SettingsDataStore,
) : SpeechRecognitionEngine {

    private val initMutex = Mutex()

    @Volatile
    private var model: Model? = null

    @Volatile
    private var loadedLanguage: VoiceLanguage? = null

    @Volatile
    private var endOfUtteranceRequested: Boolean = false

    override fun requestEndOfUtterance() {
        endOfUtteranceRequested = true
    }

    override suspend fun initialize(): Result<Unit> = initMutex.withLock {
        val language = VoiceLanguage.fromStorageKey(
            settingsDataStore.settings.first().voiceLanguage,
        )
        if (model != null && loadedLanguage == language) {
            return Result.success(Unit)
        }
        withContext(ioDispatcher) {
            runCatching {
                releaseModelLocked()
                val assetDir = language.voskAssetDir
                val modelDir = File(context.filesDir, assetDir)
                if (!modelDir.resolve(MODEL_READY_MARKER).exists()) {
                    copyAssetDir(assetDir, modelDir)
                    modelDir.resolve(MODEL_READY_MARKER).createNewFile()
                }
                require(modelDir.resolve("conf/model.conf").exists()) {
                    "Vosk model missing conf/model.conf under assets/$assetDir"
                }
                model = Model(modelDir.absolutePath)
                loadedLanguage = language
                Log.i(TAG, "Loaded Vosk model for ${language.storageKey} ($assetDir)")
            }.onFailure {
                Log.e(TAG, "Voice model initialization failed for $language", it)
                releaseModelLocked()
            }.map { }
        }
    }

    /**
     * Nhận khung PCM từ [com.viva.voice.audio.AudioCapture]; **không** mở microphone.
     *
     * Bản trước dựng `AudioRecord` riêng ngay trong đây, nên Silero VAD không có cách
     * nào nhìn thấy cùng dòng audio mà Vosk đang nghe — muốn cắm VAD vào thì phải mở
     * mic thứ hai, và trên AAOS cái mở sau bị từ chối. Từ đây engine chỉ là một
     * consumer của [frames] (28-PIPELINE §8 P0.2).
     */
    override fun transcribe(frames: Flow<PcmFrame>): Flow<TranscriptionEvent> = flow {
        val loadedModel = model
        val language = loadedLanguage
        if (loadedModel == null || language == null) {
            emit(
                TranscriptionEvent.Error(
                    code = TranscriptionEvent.CODE_MODEL_UNAVAILABLE,
                    diagnostic = "Vosk model not loaded; check offline STT assets for the selected language",
                ),
            )
            return@flow
        }

        endOfUtteranceRequested = false
        val recognizer = Recognizer(loadedModel, SAMPLE_RATE.toFloat())
        val startedAtMs = SystemClock.elapsedRealtime()
        Log.d(TAG, "Nhan khung PCM voi Vosk ${language.storageKey}")

        try {
            var finished = false
            var lastPartial = ""

            // Đo biên độ đầu vào, mỗi giây một dòng.
            //
            // Khi một lượt kết thúc bằng `Error:speech_end`, có hai nguyên nhân
            // hoàn toàn khác nhau mà log cũ không phân biệt được: mic không đẩy
            // mẫu nào (toàn 0), hay mic vẫn chạy nhưng Vosk không bao giờ chốt
            // câu. Cách xử lý của hai thứ đó khác hẳn nhau, nên đừng đoán.
            var framedSamples = 0
            var peakInWindow = 0

            // Vị ngữ chạy **trước** mỗi khung, nên khi thân vòng đặt `finished` thì
            // khung kế tiếp dừng cả chuỗi — kể cả AudioCapture ở đầu nguồn.
            frames.takeWhile { !finished }.collect { frame ->
                val samples = frame.samples
                if (endOfUtteranceRequested) {
                    // VAD đã thấy biên (hoặc nút đã nhả): chốt câu ngay, không chờ
                    // Vosk tự quyết định. Đây là điều làm cho endpoint của lượt là
                    // endpoint do VAD đo, chứ không phải hai endpointer cãi nhau.
                    finished = true
                    emit(finalEvent(recognizer, lastPartial, startedAtMs))
                    return@collect
                }

                for (sample in samples) {
                    val amplitude = kotlin.math.abs(sample.toInt())
                    if (amplitude > peakInWindow) peakInWindow = amplitude
                }
                framedSamples += samples.size
                if (framedSamples >= SAMPLE_RATE) {
                    Log.d(TAG, "Muc dau vao: peak=$peakInWindow/32767 (partial=\"$lastPartial\")")
                    framedSamples = 0
                    peakInWindow = 0
                }

                if (recognizer.acceptWaveForm(samples, samples.size)) {
                    val text = JSONObject(recognizer.result).optString("text").trim()
                    Log.d(TAG, "Vosk chot cau: \"$text\"")
                    if (text.isNotEmpty()) {
                        finished = true
                        emit(
                            TranscriptionEvent.Final(
                                text = text,
                                acousticConfidence = null,
                                engineMs = elapsedSince(startedAtMs),
                            ),
                        )
                    }
                } else {
                    val partial = JSONObject(recognizer.partialResult).optString("partial").trim()
                    if (partial.isNotEmpty() && partial != lastPartial) {
                        Log.d(TAG, "Partial: \"$partial\"")
                        lastPartial = partial
                        emit(TranscriptionEvent.Partial(partial))
                    }
                }
            }

            // Nguồn audio hết trước khi có câu: chạm trần thời lượng, mic chết, hoặc
            // session bị đóng. Vẫn phải chốt cái đang có, không im lặng.
            if (!finished) {
                emit(finalEvent(recognizer, lastPartial, startedAtMs))
            }
        } finally {
            recognizer.close()
        }
    }.flowOn(ioDispatcher)

    /** Kết quả cuối khi bị ép chốt: `finalResult` của Vosk, hoặc partial gần nhất. */
    private fun finalEvent(
        recognizer: Recognizer,
        lastPartial: String,
        startedAtMs: Long,
    ): TranscriptionEvent {
        val forced = JSONObject(recognizer.finalResult).optString("text").trim()
            .ifEmpty { lastPartial }
        return if (forced.isNotEmpty()) {
            TranscriptionEvent.Final(
                text = forced,
                // Vosk small không trả confidence đã hiệu chỉnh. `null` là câu trả lời
                // trung thực; xem TranscriptionEvent.Final.acousticConfidence.
                acousticConfidence = null,
                engineMs = elapsedSince(startedAtMs),
            )
        } else {
            TranscriptionEvent.Error(
                code = TranscriptionEvent.CODE_NO_SPEECH,
                diagnostic = "recognizer produced no text before the endpoint",
            )
        }
    }

    private fun elapsedSince(startedAtMs: Long): Int =
        (SystemClock.elapsedRealtime() - startedAtMs).toInt().coerceAtLeast(0)

    private fun releaseModelLocked() {
        runCatching { model?.close() }
        model = null
        loadedLanguage = null
    }

    private fun copyAssetDir(assetPath: String, target: File) {
        val assets = context.assets
        val children = assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            if (assetPath == "model-en-us" || assetPath == "model-vi") {
                error("Vosk model not found in assets/$assetPath")
            }
            target.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            target.mkdirs()
            children.forEach { child ->
                copyAssetDir("$assetPath/$child", File(target, child))
            }
        }
    }

    private companion object {
        const val TAG = "VoskEngine"
        const val MODEL_READY_MARKER = ".unpacked"
        const val SAMPLE_RATE = 16_000
    }
}
