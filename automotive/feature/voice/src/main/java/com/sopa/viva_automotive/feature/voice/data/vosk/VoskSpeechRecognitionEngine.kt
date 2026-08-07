package com.sopa.viva_automotive.feature.voice.data.vosk

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
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

    /**
     * Model đang nạp có **đồ thị động** hay không, quyết định lúc [initialize].
     *
     * Chỉ model dựng theo kiểu `HCLr.fst` + `Gr.fst` mới nhận được grammar lúc
     * chạy. Với model đồ thị tĩnh (`HCLG.fst` — ví dụ `vosk-model-vn-0.4` bản
     * lớn), Vosk **âm thầm bỏ qua** grammar truyền vào và vẫn giải mã trên toàn
     * bộ vốn từ. Không có cờ này thì việc thu hẹp vốn từ trông như đang chạy
     * trong khi thực tế không làm gì cả — đúng cái bẫy đã mất một buổi để phát
     * hiện. Kiểm sự tồn tại của file rồi ghi log, thay vì tin.
     */
    @Volatile
    private var hasDynamicGraph: Boolean = false

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
                hasDynamicGraph = modelDir.resolve("graph/Gr.fst").exists()
                Log.i(TAG, "Loaded Vosk model for ${language.storageKey} ($assetDir)")
                Log.i(
                    TAG,
                    if (hasDynamicGraph) {
                        "Do thi dong (Gr.fst): rang buoc von tu ${CommandVocabulary.size} tu SE co hieu luc"
                    } else {
                        "Do thi TINH (khong co Gr.fst): rang buoc von tu KHONG co hieu luc, " +
                            "giai ma tren toan bo von tu cua model"
                    },
                )
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

        // Ràng vốn từ ngay ở decoder, không phải sửa lỗi ở tầng trên.
        //
        // Đây là chỗ duy nhất sửa được đúng nguyên nhân của nhóm lỗi
        // "giảm tốc độ quạt xuống hai" → "giảm nhiệt lũ quét súng hơi hay":
        // mô hình ngôn ngữ tổng quát kéo kết quả về phía chuỗi từ phổ biến
        // trong văn bản đời thường. `FuzzyCommandMatcher` chỉ dọn được phần
        // sai lệch nhẹ; nó không thể dựng lại một câu mà decoder đã vứt đi.
        // Xem [CommandVocabulary] về việc vì sao là danh sách từ chứ không
        // phải danh sách câu, và vì sao `[unk]` là bắt buộc.
        val useGrammar = hasDynamicGraph &&
            language == VoiceLanguage.VIETNAMESE &&
            grammarEnabled()
        val recognizer = if (useGrammar) {
            Recognizer(loadedModel, SAMPLE_RATE.toFloat(), CommandVocabulary.asGrammarJson())
        } else {
            Recognizer(loadedModel, SAMPLE_RATE.toFloat())
        }
        Log.d(TAG, "Recognizer: grammar=${if (useGrammar) "BAT" else "TAT"}")

        // Bật confidence theo từng từ — hiện chỉ để ĐO, chưa dùng để quyết định.
        //
        // `VoiceTurnReport.MIN_ACOUSTIC_CONFIDENCE` tồn tại từ đầu nhưng là luật
        // chết: Vosk mặc định không trả confidence, nên `needsRepeatForConfidence`
        // luôn nhận `null` và luôn trả false. `setWords(true)` làm `finalResult`
        // kèm mảng `result[]` có `conf` cho từng từ, tức con số đó tồn tại thật.
        //
        // Cố ý CHƯA nối vào đường quyết định. Chọn ngưỡng mà không có bộ 20–30
        // lượt nói thật thì chỉ là đoán, và đoán sai ở đây nghĩa là trợ lý hỏi
        // lại một lệnh nó đã nghe đúng. Ghi log trước, chỉnh ngưỡng theo số đo
        // sau — đúng thứ tự.
        recognizer.setWords(true)
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
            var windowStartedAtMs = SystemClock.elapsedRealtime()

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
                    // Tỉ lệ audio-trên-thời-gian-thực, in kèm chứ không để người
                    // đọc tự nhẩm từ dấu thời gian.
                    //
                    // `AudioRecord.read` là blocking: một microphone THẬT không
                    // thể đẩy nhanh hơn 1,0×. Thấy 3× nghĩa là nguồn đang chạy
                    // tự do — trên emulator đó là mic ảo không nối vào audio của
                    // máy host, và nó trả về rác chứ không phải im lặng. Triệu
                    // chứng khi đó y hệt "ASR nghe kém": biên độ lớn, transcript
                    // rỗng, VAD không thấy tiếng nói. Không có con số này thì ba
                    // thứ đó dẫn người đọc đi sai đường.
                    val wallMs = SystemClock.elapsedRealtime() - windowStartedAtMs
                    val ratio = if (wallMs > 0) 1000.0 * framedSamples / SAMPLE_RATE / wallMs else 0.0
                    Log.d(
                        TAG,
                        "Muc dau vao: peak=$peakInWindow/32767 " +
                            "toc do=${String.format("%.1f", ratio)}x thoi gian thuc " +
                            "(partial=\"$lastPartial\")",
                    )
                    framedSamples = 0
                    peakInWindow = 0
                    windowStartedAtMs = SystemClock.elapsedRealtime()
                }

                if (recognizer.acceptWaveForm(samples, samples.size)) {
                    val raw = recognizer.result
                    val text = JSONObject(raw).optString("text").trim()
                    // Ghi cả JSON thô khi rỗng: `text` rỗng vẫn có thể đi kèm
                    // `result[]` chứa `[unk]`. Hai thứ đó nói hai chuyện khác hẳn
                    // nhau — có `[unk]` nghĩa là decoder CÓ nghe thấy tiếng nói
                    // nhưng grammar từ chối; rỗng hoàn toàn nghĩa là tiếng nói
                    // chưa từng tới được decoder. Không có dòng này thì cả hai
                    // trông giống nhau y hệt trong log.
                    if (text.isEmpty()) {
                        Log.d(TAG, "Vosk chot cau RONG, JSON tho: $raw")
                    }
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
        val result = JSONObject(recognizer.finalResult)
        val forced = result.optString("text").trim().ifEmpty { lastPartial }
        logWordConfidences(result)
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

    /**
     * Công tắc tắt ràng buộc vốn từ **mà không phải build lại**.
     *
     * ```
     * adb shell settings put global viva_asr_grammar 0   # tắt
     * adb shell settings delete global viva_asr_grammar  # bật lại (mặc định)
     * ```
     *
     * Lý do tồn tại: khi một lượt nói ra transcript rỗng, có hai nguyên nhân
     * đòi hai cách sửa ngược nhau — grammar chặn nhầm tiếng nói hợp lệ, hay
     * tầng audio/VAD không đưa được tiếng nói tới decoder. Phân biệt chúng cần
     * đọc **cùng một giọng nói** qua cả hai cấu hình; không có công tắc này thì
     * mỗi lần đổi phải build lại và giọng nói đã khác đi.
     *
     * Mặc định bật. Đọc lỗi thì coi như bật, vì mất ràng buộc âm thầm còn tệ
     * hơn: nó đưa hệ thống về đúng hành vi cũ mà không ai biết.
     */
    private fun grammarEnabled(): Boolean = runCatching {
        Settings.Global.getInt(context.contentResolver, SETTING_GRAMMAR, 1) == 1
    }.getOrDefault(true)

    /**
     * Ghi `conf` từng từ mà `setWords(true)` sinh ra, kèm giá trị nhỏ nhất.
     *
     * Đây là dữ liệu để chọn ngưỡng cho `VoiceTurnReport.MIN_ACOUSTIC_CONFIDENCE`
     * sau một phiên nói thật: từ yếu nhất trong câu mới là thứ nói lên câu đó có
     * đáng tin hay không, chứ không phải trung bình — một lệnh mở khoá cửa nghe
     * đúng chín từ và sai từ "mở" vẫn là một lệnh không được phép chạy.
     *
     * Chỉ ghi log, không đổi kết quả trả về.
     */
    private fun logWordConfidences(result: JSONObject) {
        val words = result.optJSONArray("result") ?: return
        if (words.length() == 0) return
        var min = Double.MAX_VALUE
        val parts = StringBuilder()
        for (i in 0 until words.length()) {
            val w = words.optJSONObject(i) ?: continue
            val conf = w.optDouble("conf", Double.NaN)
            if (conf.isNaN()) continue
            if (conf < min) min = conf
            if (parts.isNotEmpty()) parts.append(' ')
            parts.append(w.optString("word")).append('=').append(String.format("%.2f", conf))
        }
        if (min == Double.MAX_VALUE) return
        Log.d(TAG, "Conf tung tu: $parts | thap nhat=${String.format("%.2f", min)}")
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

        /** Khoa trong `Settings.Global` de tat rang buoc von tu luc chay. */
        const val SETTING_GRAMMAR = "viva_asr_grammar"
    }
}
