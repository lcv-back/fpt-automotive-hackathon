package com.sopa.viva_automotive.feature.voice.domain.embedding

import kotlin.math.sqrt

object CosineSimilarity {

    fun score(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vector size mismatch: ${a.size} vs ${b.size}" }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        if (denom == 0f) return 0f
        return dot / denom
    }

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0f
        for (v in vector) sumSq += v * v
        val norm = sqrt(sumSq)
        if (norm == 0f) return vector.copyOf()
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }
}
