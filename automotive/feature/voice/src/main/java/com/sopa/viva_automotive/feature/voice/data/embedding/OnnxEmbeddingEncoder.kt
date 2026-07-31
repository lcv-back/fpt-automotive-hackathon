package com.sopa.viva_automotive.feature.voice.data.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.sopa.viva_automotive.core.common.coroutines.IoDispatcher
import com.sopa.viva_automotive.feature.voice.domain.embedding.CosineSimilarity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class OnnxEmbeddingEncoder @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val mutex = Mutex()

    @Volatile
    private var session: OrtSession? = null

    @Volatile
    private var tokenizer: BertWordPieceTokenizer? = null

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    suspend fun ensureReady(): Boolean = mutex.withLock {
        if (session != null && tokenizer != null) return true
        withContext(ioDispatcher) {
            runCatching {
                unpackAssetsIfNeeded()
                val modelFile = File(modelDir(), MODEL_FILE)
                val vocabFile = File(modelDir(), VOCAB_FILE)
                require(modelFile.exists() && vocabFile.exists()) {
                    "Embedding assets missing under ${modelDir().absolutePath}"
                }
                val options = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(1)
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                }
                session = env.createSession(modelFile.absolutePath, options)
                tokenizer = BertWordPieceTokenizer.fromVocabLines(
                    vocabFile.readLines(Charsets.UTF_8),
                    maxLength = MAX_SEQ_LEN,
                )
                Log.i(TAG, "MiniLM embedding encoder ready (${modelFile.length() / 1024} KB)")
            }.onFailure {
                Log.e(TAG, "Failed to init embedding encoder", it)
                session = null
                tokenizer = null
            }.isSuccess
        }
    }

    suspend fun embed(text: String): FloatArray? {
        if (!ensureReady()) return null
        return withContext(ioDispatcher) {
            mutex.withLock {
                val tok = tokenizer ?: return@withLock null
                val ort = session ?: return@withLock null
                val encoding = tok.encode(text)
                runCatching { infer(ort, encoding) }
                    .onFailure { Log.e(TAG, "Embedding inference failed", it) }
                    .getOrNull()
            }
        }
    }

    private fun infer(ort: OrtSession, encoding: BertWordPieceTokenizer.Encoding): FloatArray {
        val shape = longArrayOf(1, encoding.inputIds.size.toLong())
        OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.inputIds), shape).use { inputIds ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.attentionMask), shape).use { attention ->
                OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.tokenTypeIds), shape).use { typeIds ->
                    val inputs = mapOf(
                        "input_ids" to inputIds,
                        "attention_mask" to attention,
                        "token_type_ids" to typeIds,
                    )
                    ort.run(inputs).use { result ->
                        return meanPoolAndNormalize(result[0].value, encoding.attentionMask)
                    }
                }
            }
        }
    }

        private fun meanPoolAndNormalize(raw: Any, attentionMask: LongArray): FloatArray {
        val seqHidden = extractSequence(raw)
        val dim = seqHidden[0].size
        val pooled = FloatArray(dim)
        var counted = 0f
        val seqLen = minOf(seqHidden.size, attentionMask.size)
        for (i in 0 until seqLen) {
            if (attentionMask[i] == 0L) continue
            val row = seqHidden[i]
            for (d in 0 until dim) pooled[d] += row[d]
            counted += 1f
        }
        if (counted > 0f) {
            for (d in 0 until dim) pooled[d] /= counted
        }
        return CosineSimilarity.l2Normalize(pooled)
    }

    private fun extractSequence(raw: Any): Array<FloatArray> {
        val batch0 = (raw as Array<*>)[0]
        @Suppress("UNCHECKED_CAST")
        return when (batch0) {
            is Array<*> -> batch0 as Array<FloatArray>
            else -> error("Unexpected hidden batch type: ${batch0?.javaClass}")
        }
    }

    private fun unpackAssetsIfNeeded() {
        val dir = modelDir()
        val ready = File(dir, READY_MARKER)
        if (ready.exists() && File(dir, MODEL_FILE).exists() && File(dir, VOCAB_FILE).exists()) {
            return
        }
        dir.mkdirs()
        copyAsset(MODEL_FILE, File(dir, MODEL_FILE))
        copyAsset(VOCAB_FILE, File(dir, VOCAB_FILE))
        ready.writeText("ok")
    }

    private fun copyAsset(name: String, target: File) {
        context.assets.open("$ASSET_DIR/$name").use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun modelDir(): File = File(context.filesDir, ASSET_DIR)

    private companion object {
        const val TAG = "OnnxEmbed"
        const val ASSET_DIR = "embeddings"
        const val MODEL_FILE = "model_quantized.onnx"
        const val VOCAB_FILE = "vocab.txt"
        const val READY_MARKER = ".unpacked"
        const val MAX_SEQ_LEN = 64
    }
}
