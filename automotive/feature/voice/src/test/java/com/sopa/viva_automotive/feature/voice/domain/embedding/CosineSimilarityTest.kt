package com.sopa.viva_automotive.feature.voice.domain.embedding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CosineSimilarityTest {

    @Test
    fun `identical vectors score 1`() {
        val v = floatArrayOf(0.6f, 0.8f)
        assertEquals(1f, CosineSimilarity.score(v, v), 1e-5f)
    }

    @Test
    fun `orthogonal vectors score 0`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0f, CosineSimilarity.score(a, b), 1e-5f)
    }

    @Test
    fun `l2 normalize unit length`() {
        val n = CosineSimilarity.l2Normalize(floatArrayOf(3f, 4f))
        assertEquals(0.6f, n[0], 1e-5f)
        assertEquals(0.8f, n[1], 1e-5f)
        val norm = kotlin.math.sqrt(n[0] * n[0] + n[1] * n[1])
        assertEquals(1f, norm, 1e-5f)
    }

    @Test
    fun `similar directions score high`() {
        val a = CosineSimilarity.l2Normalize(floatArrayOf(1f, 2f, 3f))
        val b = CosineSimilarity.l2Normalize(floatArrayOf(1.1f, 2.0f, 2.9f))
        assertTrue(CosineSimilarity.score(a, b) > 0.99f)
    }
}
