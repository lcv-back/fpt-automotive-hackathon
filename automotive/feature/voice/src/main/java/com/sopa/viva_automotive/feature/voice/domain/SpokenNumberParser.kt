package com.sopa.viva_automotive.feature.voice.domain

object SpokenNumberParser {

    private val EN_UNITS = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19,
    )

    private val EN_TENS = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
    )

    private val VI_UNITS = mapOf(
        "không" to 0, "một" to 1, "mốt" to 1, "hai" to 2, "ba" to 3, "bốn" to 4,
        "tư" to 4, "năm" to 5, "lăm" to 5, "sáu" to 6, "bảy" to 7, "bẩy" to 7,
        "tám" to 8, "chín" to 9,
    )

    fun parse(text: String): Double? {
        val normalized = text.lowercase().replace('-', ' ')

        Regex("""-?\d+(?:\.\d+)?""").find(normalized)?.let { return it.value.toDoubleOrNull() }

        parseVietnamese(normalized)?.let { return it }
        parseEnglish(normalized)?.let { return it }
        return null
    }

    private fun parseEnglish(normalized: String): Double? {
        val words = normalized.split(Regex("""\s+"""))
        var index = 0
        while (index < words.size) {
            val word = words[index]
            var value: Int? = null
            var consumed = 1
            when {
                word in EN_TENS -> {
                    value = EN_TENS.getValue(word)
                    val next = words.getOrNull(index + 1)
                    if (next != null && next in EN_UNITS && EN_UNITS.getValue(next) < 10) {
                        value += EN_UNITS.getValue(next)
                        consumed = 2
                    }
                }
                word in EN_UNITS -> value = EN_UNITS.getValue(word)
            }
            if (value != null) {
                var result = value.toDouble()
                val tail = words.drop(index + consumed)
                if (tail.getOrNull(0) == "point" && tail.getOrNull(1) == "five") {
                    result += 0.5
                }
                if (tail.getOrNull(0) == "and" && tail.getOrNull(1) == "a" && tail.getOrNull(2) == "half") {
                    result += 0.5
                }
                return result
            }
            index++
        }
        return null
    }

        private fun parseVietnamese(normalized: String): Double? {
        val words = normalized.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        var index = 0
        while (index < words.size) {
            val parsed = parseVietnameseAt(words, index)
            if (parsed == null) {
                index++
                continue
            }
            var result = parsed.value.toDouble()
            val tail = words.drop(index + parsed.consumed)
            when {
                tail.getOrNull(0) == "rưỡi" -> result += 0.5
                tail.getOrNull(0) == "phẩy" && (tail.getOrNull(1) == "năm" || tail.getOrNull(1) == "lăm") ->
                    result += 0.5
            }
            return result
        }
        return null
    }

    private data class ViParse(val value: Int, val consumed: Int)

    private fun parseVietnameseAt(words: List<String>, index: Int): ViParse? {
        val w0 = words.getOrNull(index) ?: return null
        val w1 = words.getOrNull(index + 1)
        val w2 = words.getOrNull(index + 2)

        if (w0 == "mười") {
            val unit = w1?.let { VI_UNITS[it] }
            return if (unit != null && unit in 1..9) {
                ViParse(10 + unit, 2)
            } else {
                ViParse(10, 1)
            }
        }

        if (w1 == "mươi") {
            val tensUnit = VI_UNITS[w0] ?: return null
            if (tensUnit !in 2..9) return null
            val tens = tensUnit * 10
            val ones = w2?.let { VI_UNITS[it] }
            return if (ones != null && ones in 1..9) {
                ViParse(tens + ones, 3)
            } else {
                ViParse(tens, 2)
            }
        }

        val unit = VI_UNITS[w0] ?: return null
        return ViParse(unit, 1)
    }
}
