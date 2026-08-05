package com.sopa.viva_automotive.feature.voice.data.vosk

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.sopa.viva_automotive.core.common.coroutines.IoDispatcher
import com.sopa.viva_automotive.core.database.settings.SettingsDataStore
import com.sopa.viva_automotive.core.ui.locale.VoiceLanguage
import com.sopa.viva_automotive.feature.voice.data.SpeechRecognitionEngine
import com.sopa.viva_automotive.feature.voice.data.TranscriptionEvent
import com.viva.voice.intent.CommandVocabulary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    @SuppressLint("MissingPermission") // RECORD_AUDIO is checked by the caller (service/UI).
    override fun transcribe(): Flow<TranscriptionEvent> = callbackFlow {
        val loadedModel = model
        val language = loadedLanguage
        if (loadedModel == null || language == null) {
            trySend(
                TranscriptionEvent.Error(
                    "Voice model is not available. Check offline STT assets for the selected language.",
                ),
            )
            close()
            return@callbackFlow
        }

        endOfUtteranceRequested = false
        val recognizer = createRecognizer(loadedModel, language)
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 2, BUFFER_SIZE_SAMPLES * 2),
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            recognizer.close()
            audioRecord.release()
            trySend(TranscriptionEvent.Error("Microphone is unavailable"))
            close()
            return@callbackFlow
        }

        audioRecord.startRecording()
        Log.d(TAG, "Listening with Vosk ${language.storageKey}")

        val readerJob = launch(ioDispatcher) {
            val buffer = ShortArray(BUFFER_SIZE_SAMPLES)
            var lastPartial = ""

            // Đo biên độ đầu vào, mỗi giây một dòng.
            //
            // Khi một lượt kết thúc bằng `Error:speech_end`, có hai nguyên nhân
            // hoàn toàn khác nhau mà log cũ không phân biệt được: mic không đẩy
            // mẫu nào (toàn 0), hay mic vẫn chạy nhưng Vosk không bao giờ chốt
            // câu. Cách xử lý của hai thứ đó khác hẳn nhau, nên đừng đoán.
            var blocks = 0
            var peakInWindow = 0
            while (isActive) {
                if (endOfUtteranceRequested) {
                    val forced = JSONObject(recognizer.finalResult).optString("text").trim()
                        .ifEmpty { lastPartial }
                    if (forced.isNotEmpty()) {
                        send(TranscriptionEvent.Final(forced))
                    } else {
                        send(TranscriptionEvent.Error("I didn't hear anything"))
                    }
                    break
                }

                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read < 0) {
                    send(TranscriptionEvent.Error("Microphone read failed (code $read)"))
                    break
                }
                if (read == 0) continue

                for (i in 0 until read) {
                    val amplitude = kotlin.math.abs(buffer[i].toInt())
                    if (amplitude > peakInWindow) peakInWindow = amplitude
                }
                blocks++
                if (blocks * BUFFER_SIZE_SAMPLES >= SAMPLE_RATE) {
                    Log.d(TAG, "Muc dau vao: peak=$peakInWindow/32767 (partial=\"$lastPartial\")")
                    blocks = 0
                    peakInWindow = 0
                }

                if (recognizer.acceptWaveForm(buffer, read)) {
                    val text = JSONObject(recognizer.result).optString("text").trim()
                    Log.d(TAG, "Vosk chot cau: \"$text\"")
                    if (text.isNotEmpty()) {
                        send(TranscriptionEvent.Final(text))
                        break
                    }
                } else {
                    val partial = JSONObject(recognizer.partialResult).optString("partial").trim()
                    if (partial.isNotEmpty() && partial != lastPartial) {
                        Log.d(TAG, "Partial: \"$partial\"")
                        lastPartial = partial
                        send(TranscriptionEvent.Partial(partial))
                    }
                }
            }
            close()
        }

        awaitClose {
            endOfUtteranceRequested = true
            readerJob.cancel()
            runCatching { audioRecord.stop() }
            audioRecord.release()
            recognizer.close()
        }
    }.flowOn(ioDispatcher)

    /**
     * Dựng recognizer, và **chỉ giới hạn vốn từ khi model thật sự hỗ trợ**.
     *
     * ## Vì sao phải kiểm, không được cứ thế truyền grammar vào
     *
     * `Recognizer(model, rate, grammar)` chỉ có tác dụng với model dựng theo
     * **đồ thị động** (`graph/HCLr.fst` + `graph/Gr.fst`). Với model dựng sẵn
     * đồ thị tĩnh (`graph/HCLG.fst`), Vosk **âm thầm bỏ qua** grammar và vẫn
     * giải mã bằng toàn bộ ngôn ngữ mô hình — không lỗi, không cảnh báo.
     *
     * Đo trên máy 05/08:
     * ```
     * model-vi    : HCLG.fst                              -> KHÔNG hỗ trợ
     * model-en-us : HCLr.fst + Gr.fst + disambig_tid.int   -> có hỗ trợ
     * ```
     * Truyền grammar cho `vn-0.4` rồi log "đã giới hạn 76 từ" là tự khai một
     * thứ không xảy ra: transcript sau đó vẫn chứa `bây giờ`, `muốn`, `hạ vi` —
     * toàn từ ngoài danh sách. Một dòng log sai kiểu đó nguy hiểm hơn không có
     * log, vì nó làm cả đội tin rằng đã sửa xong.
     *
     * Vốn từ vẫn được giữ trong [CommandVocabulary]: nó là cơ sở cho lớp khớp
     * mờ ở tầng NLU, và nếu sau này thay bằng model tiếng Việt dựng đồ thị động
     * thì ràng buộc tự bật lên mà không phải sửa gì ở đây.
     */
    private fun createRecognizer(model: Model, language: VoiceLanguage): Recognizer {
        val open = Recognizer(model, SAMPLE_RATE.toFloat())
        if (language != VoiceLanguage.VIETNAMESE) return open

        if (!supportsRuntimeGrammar(language)) {
            Log.i(
                TAG,
                "Model ${language.voskAssetDir} dung do thi tinh (HCLG.fst) nen KHONG gioi han " +
                    "duoc von tu — giai ma mo tren toan bo ngon ngu mo hinh.",
            )
            return open
        }

        return runCatching {
            open.close()
            Recognizer(model, SAMPLE_RATE.toFloat(), CommandVocabulary.asVoskGrammar()).also {
                Log.i(TAG, "Da gioi han von tu: ${CommandVocabulary.words.size} tu + [unk]")
            }
        }.getOrElse { error ->
            Log.w(TAG, "Khong dung duoc grammar, quay ve giai ma mo", error)
            Recognizer(model, SAMPLE_RATE.toFloat())
        }
    }

    /** Đồ thị động mới nhận grammar lúc chạy; đồ thị tĩnh thì không. */
    private fun supportsRuntimeGrammar(language: VoiceLanguage): Boolean =
        File(context.filesDir, language.voskAssetDir).resolve("graph/Gr.fst").exists()

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
        const val BUFFER_SIZE_SAMPLES = 4_096
    }
}
