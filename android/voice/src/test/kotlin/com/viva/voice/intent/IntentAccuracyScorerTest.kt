package com.viva.voice.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentAccuracyScorerTest {

    private val scorer = IntentAccuracyScorer(GrammarIntentRouter())

    @Test
    fun `identical transcript scores as correct`() {
        val rows = listOf(
            IntentAccuracyScorer.Row("c1", "đặt nhiệt độ 24 độ", "đặt nhiệt độ 24 độ"),
        )
        val score = scorer.score(rows)
        assertEquals(1, score.total)
        assertEquals(1, score.correct)
    }

    @Test
    fun `harmless misspelling that still routes the same is correct`() {
        // "dat"->"dac" is the exact error class the ASR manifest recorded. The
        // router keys on "nhiệt độ" + the number, so the action is unchanged.
        val rows = listOf(
            IntentAccuracyScorer.Row("c1", "đặt nhiệt độ 24 độ", "đạc nhiệt độ 24 độ"),
        )
        assertEquals(1, scorer.score(rows).correct)
    }

    @Test
    fun `wrong slot value counts as incorrect`() {
        val rows = listOf(
            IntentAccuracyScorer.Row("c1", "đặt nhiệt độ 24 độ", "đặt nhiệt độ 20 độ"),
        )
        val score = scorer.score(rows)
        assertEquals(1, score.total)
        assertEquals(0, score.correct)
    }

    @Test
    fun `blank hypothesis counts as incorrect not as skipped`() {
        // tts_volume_up returned "" in the last run. A blank must land in the
        // sample as a failure, never be filtered out.
        val rows = listOf(
            IntentAccuracyScorer.Row("c1", "tăng âm lượng", ""),
        )
        val score = scorer.score(rows)
        assertEquals(1, score.total)
        assertEquals(0, score.correct)
    }

    @Test
    fun `outcome key includes intent name and slots`() {
        val matched = GrammarIntentRouter().route("đặt nhiệt độ 24 độ")
        val key = scorer.outcomeKey(matched)
        assertTrue("unexpected key: $key", key.startsWith("matched:hvac_set_temp"))
        assertTrue("slots missing from key: $key", key.contains("24"))
    }

    @Test
    fun `csv parser handles quoted fields containing commas`() {
        val csv = """
            clip,reference,hypothesis
            c1,"hạ điều hòa, xuống 24 độ","hạ điều hòa xuống 24 độ"
        """.trimIndent()
        val rows = IntentAccuracyScorer.parseCsv(csv)
        assertEquals(1, rows.size)
        assertEquals("hạ điều hòa, xuống 24 độ", rows[0].reference)
        assertEquals("hạ điều hòa xuống 24 độ", rows[0].hypothesis)
    }

    @Test
    fun `csv parser handles escaped double quotes`() {
        val csv = "clip,reference,hypothesis\nc1,\"nói \"\"viva ơi\"\"\",\"nói viva ơi\""
        val rows = IntentAccuracyScorer.parseCsv(csv)
        assertEquals("nói \"viva ơi\"", rows[0].reference)
    }

    /**
     * Scores a real bench CSV when one is handed in:
     *
     *   ./gradlew :voice-core:testDebugUnitTest -Dviva.bench.csv=/abs/path/asr-bench.csv
     *
     * Skipped otherwise so CI stays green without the corpus.
     */
    @Test
    fun `score a bench csv when the path is supplied`() {
        val path = System.getProperty("viva.bench.csv") ?: return
        val file = java.io.File(path)
        assertTrue("bench CSV not found: $path", file.exists())

        val rows = IntentAccuracyScorer.parseCsv(file.readText(Charsets.UTF_8))
        val score = scorer.score(rows)
        println("VIVA_INTENT_ACCURACY rows=${score.total} correct=${score.correct} " +
            "accuracy=${"%.4f".format(score.accuracy)} reference_unroutable=${score.referenceUnroutable}")
        assertTrue("bench CSV had no scorable rows", score.total > 0)
    }
}
