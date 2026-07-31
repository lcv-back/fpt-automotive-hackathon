package com.viva.voice.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.Closeable
import java.nio.LongBuffer

/** Stateful Silero VAD v6.2.1 scorer for fixed 512-sample, 16 kHz PCM16 frames. */
class SileroVadOnnxScorer(
    context: Context,
    assetName: String = MODEL_ASSET,
) : VoiceActivityScorer, Closeable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = context.assets.open(assetName).use { asset ->
        val modelBytes = asset.readBytes()
        // ONNX Runtime's Java API accepts a model byte array and a named tensor map.
        // https://onnxruntime.ai/docs/get-started/with-java.html
        OrtSession.SessionOptions().use { options ->
            environment.createSession(modelBytes, options)
        }
    }
    private var state = newState()
    private var contextSamples = FloatArray(CONTEXT_SAMPLES)

    init {
        check(session.inputNames == setOf(INPUT, STATE, SAMPLE_RATE)) {
            "Unexpected Silero inputs: ${session.inputNames}"
        }
    }

    @Synchronized
    override fun probability(frame: ShortArray): Float {
        require(frame.size == FRAME_SAMPLES) {
            "Silero VAD expects $FRAME_SAMPLES samples, got ${frame.size}"
        }

        val normalized = FloatArray(FRAME_SAMPLES) { index -> frame[index] / 32_768f }
        val modelInput = Array(1) { FloatArray(CONTEXT_SAMPLES + FRAME_SAMPLES) }
        contextSamples.copyInto(modelInput[0], destinationOffset = 0)
        normalized.copyInto(modelInput[0], destinationOffset = CONTEXT_SAMPLES)

        OnnxTensor.createTensor(environment, modelInput).use { inputTensor ->
            OnnxTensor.createTensor(environment, state).use { stateTensor ->
                OnnxTensor.createTensor(
                    environment,
                    LongBuffer.wrap(longArrayOf(SAMPLE_RATE_HZ.toLong())),
                    longArrayOf(),
                ).use { sampleRateTensor ->
                    session.run(
                        mapOf(
                            INPUT to inputTensor,
                            STATE to stateTensor,
                            SAMPLE_RATE to sampleRateTensor,
                        ),
                    ).use { output ->
                        @Suppress("UNCHECKED_CAST")
                        val probabilities = output[0].value as Array<FloatArray>
                        @Suppress("UNCHECKED_CAST")
                        val nextState = output[1].value as Array<Array<FloatArray>>
                        state = nextState
                        normalized.copyInto(
                            contextSamples,
                            startIndex = FRAME_SAMPLES - CONTEXT_SAMPLES,
                        )
                        return probabilities[0][0].coerceIn(0f, 1f)
                    }
                }
            }
        }
    }

    @Synchronized
    override fun reset() {
        state = newState()
        contextSamples.fill(0f)
    }

    override fun close() {
        session.close()
    }

    private fun newState(): Array<Array<FloatArray>> =
        Array(2) { Array(1) { FloatArray(STATE_SIZE) } }

    companion object {
        const val MODEL_ASSET = "silero_vad_v6_2_1.onnx"
        const val MODEL_SHA256 = "1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3"

        private const val INPUT = "input"
        private const val STATE = "state"
        private const val SAMPLE_RATE = "sr"
        private const val SAMPLE_RATE_HZ = 16_000
        private const val FRAME_SAMPLES = 512
        private const val CONTEXT_SAMPLES = 64
        private const val STATE_SIZE = 128
    }
}
