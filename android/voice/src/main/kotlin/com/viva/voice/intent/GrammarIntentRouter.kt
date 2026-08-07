package com.viva.voice.intent

import java.util.Locale

/**
 * Deterministic T0 router for the ten core intents and additive app-owned rules.
 *
 * The optional wake phrase is stripped here so the same router works with a
 * future wake-word detector and with today's push-to-talk fallback. Extension
 * rules are snapshotted at construction and run only after core rules and
 * removed-command filters.
 */
class GrammarIntentRouter(
    extensionRules: List<GrammarRule> = emptyList(),
) : IntentRouter {
    private val extensionRules = extensionRules.toList()

    override fun route(text: String): RouteResult {
        val normalized = normalize(text)
        if (UNSUPPORTED_WAKE.containsMatchIn(normalized)) {
            return RouteResult.Unsupported(
                "Từ gọi của trợ lý là “Viva ơi” hoặc “Vivi ơi”. Bạn thử lại nhé.",
                canFallback = false,
            )
        }
        val command = normalized.replaceFirst(SUPPORTED_WAKE, "").trim()
        if (command.isEmpty()) {
            return RouteResult.NeedsClarification("Bạn muốn mình thực hiện việc gì?")
        }
        if (isRemovedCommand(command)) {
            return RouteResult.Unsupported(
                promptVi = "Lệnh này chưa hỗ trợ trong bản demo. Bạn thử một lệnh điều hòa, cửa, âm thanh hoặc giao hàng nhé.",
                canFallback = false,
            )
        }

        if (command.contains("lạnh quá")) {
            return RouteResult.NeedsClarification(
                "Bạn muốn tăng nhiệt độ điều hòa lên bao nhiêu độ?",
            )
        }
        if (command.contains("nóng quá")) {
            return RouteResult.NeedsClarification(
                "Bạn muốn giảm nhiệt độ điều hòa xuống bao nhiêu độ?",
            )
        }

        if (isTemperatureCommand(command)) {
            return routeTemperature(command)
        }
        if (command.contains("quạt")) {
            return routeFan(command)
        }
        if (command.contains("mở cửa") || command.contains("mở khóa cửa")) {
            return matched("door_lock", mapOf("lock" to false))
        }
        if (command.contains("khóa cửa")) {
            return matched("door_lock", mapOf("lock" to true))
        }
        if (command.contains("tăng âm lượng")) {
            return matched("volume_adjust", mapOf("delta" to 1))
        }
        if (command.contains("giảm âm lượng")) {
            return matched("volume_adjust", mapOf("delta" to -1))
        }
        if (command.contains("dừng nhạc") || command.contains("tạm dừng nhạc")) {
            return matched("media_pause")
        }
        if (command.contains("chuyển bài") || command.contains("bài tiếp theo")) {
            return matched("media_next")
        }
        if (command.startsWith("phát nhạc") || command.startsWith("phát playlist")) {
            // Slot `query` là thứ đem đi tìm bài, nên từ loại phải bị cắt cùng với
            // động từ: "phát playlist một ngày mới" cho "một ngày mới". Bản trước
            // chỉ cắt "phát " nên mọi query đều dính "nhạc "/"playlist " ở đầu và
            // sẽ được gửi nguyên như vậy xuống trình phát.
            val query = command
                .removePrefix("phát ")
                .removePrefix("playlist ")
                .removePrefix("nhạc ")
                .takeUnless { it == "nhạc" || it == "playlist" }
            return matched("media_play", query?.let { mapOf("query" to it) }.orEmpty())
        }
        if (command.contains("chặng tiếp theo") || command.contains("điểm dừng tiếp theo")) {
            return matched("delivery_next_stop")
        }
        if (command.contains("đơn") && DELIVERY_STATUS_CUES.any(command::contains)) {
            return matched("delivery_order_status", orderIdSlot(command))
        }
        if (command.contains("xác nhận") && command.contains("giao")) {
            return matched("delivery_confirm", orderIdSlot(command))
        }
        extensionRules.forEach { rule ->
            rule.route(command)?.let { return it }
        }
        return RouteResult.Unsupported()
    }

    private fun isRemovedCommand(command: String): Boolean =
        REMOVED_COMMANDS.any { pattern -> pattern.containsMatchIn(command) }

    private fun orderIdSlot(command: String): Map<String, Any> {
        val parsedCommand = parseVietnameseNumber(command)
        val match = ORDER_ID.find(parsedCommand) ?: return emptyMap()
        val letter = match.groupValues[1]
        val numbers = match.groupValues[2].replace(Regex("""\s+"""), "")
        val orderId = (letter + numbers).uppercase(Locale.ROOT)
        return mapOf("orderId" to orderId)
    }

    private fun routeTemperature(command: String): RouteResult {
        val parsed = parseVietnameseNumber(command)
        val value = NUMBER.find(parsed)?.groupValues?.get(1)?.toIntOrNull()
            ?: return RouteResult.NeedsClarification(
                "Bạn muốn đặt nhiệt độ điều hòa ở bao nhiêu độ?",
            )
        if (value !in MIN_TEMPERATURE_C..MAX_TEMPERATURE_C) {
            return RouteResult.NeedsClarification(
                "Nhiệt độ hỗ trợ từ 16 đến 32 độ C. Bạn muốn đặt bao nhiêu độ?",
            )
        }
        return matched("hvac_set_temp", mapOf("value" to value.toFloat()))
    }

    private fun isTemperatureCommand(command: String): Boolean {
        if (command.contains("nhiệt độ")) return true
        if (!command.contains("điều hòa")) return false
        val parsed = parseVietnameseNumber(command)
        return NUMBER.containsMatchIn(parsed) || TEMPERATURE_CUES.any { cue -> command.contains(cue) }
    }

    private fun routeFan(command: String): RouteResult {
        val parsed = parseVietnameseNumber(command)
        val level = NUMBER.find(parsed)?.groupValues?.get(1)?.toIntOrNull()
            ?: return RouteResult.NeedsClarification("Bạn muốn đặt quạt ở mức mấy, từ 0 đến 5?")
        if (level !in MIN_FAN_LEVEL..MAX_FAN_LEVEL) {
            return RouteResult.NeedsClarification("Mức quạt hỗ trợ từ 0 đến 5. Bạn chọn mức nào?")
        }
        return matched("hvac_set_fan", mapOf("level" to level))
    }

    private fun matched(name: String, slots: Map<String, Any> = emptyMap()) =
        RouteResult.Matched(
            Intent(
                name = name,
                slots = slots,
                confidence = GRAMMAR_CONFIDENCE,
                tier = Intent.Tier.T0,
            ),
        )

    private fun normalize(raw: String): String {
        return raw
            .lowercase(Locale.ROOT)
            .replace(PUNCTUATION, " ")
            .replace(WHITESPACE, " ")
            .trim()
    }

    private fun parseVietnameseNumber(raw: String): String {
        var normalized = raw
        val numberMap = mapOf(
            "không" to "0",
            "một" to "1",
            "hai" to "2",
            "ba" to "3",
            "bốn" to "4",
            "năm" to "5",
            "sáu" to "6",
            "bảy" to "7",
            "tám" to "8",
            "chín" to "9",
            "mười" to "10",
            "mười một" to "11",
            "mười hai" to "12",
            "mười ba" to "13",
            "mười bốn" to "14",
            "mười lăm" to "15",
            "mười sáu" to "16",
            "mười bảy" to "17",
            "mười tám" to "18",
            "mười chín" to "19",
            "hai mươi" to "20",
            "hai mươi một" to "21", "hai mốt" to "21", "hai một" to "21",
            "hai mươi hai" to "22", "hai hai" to "22",
            "hai mươi ba" to "23", "hai ba" to "23",
            "hai mươi bốn" to "24", "hai mươi tư" to "24", "hai bốn" to "24", "hai tư" to "24",
            "hai mươi lăm" to "25", "hai lăm" to "25", "hai năm" to "25",
            "hai mươi sáu" to "26", "hai sáu" to "26",
            "hai mươi bảy" to "27", "hai bảy" to "27",
            "hai mươi tám" to "28", "hai tám" to "28",
            "hai mươi chín" to "29", "hai chín" to "29",
            "ba mươi" to "30",
            "ba mươi một" to "31", "ba mốt" to "31", "ba một" to "31",
            "ba mươi hai" to "32", "ba hai" to "32"
        )

        val sortedKeys = numberMap.keys.sortedByDescending { it.length }
        for (key in sortedKeys) {
            normalized = normalized.replace(Regex("(?<=\\s|^)$key(?=\\s|$)"), numberMap[key]!!)
        }

        return normalized
    }

    companion object {
        private const val MIN_TEMPERATURE_C = 16
        private const val MAX_TEMPERATURE_C = 32
        private const val MIN_FAN_LEVEL = 0
        private const val MAX_FAN_LEVEL = 5
        private const val GRAMMAR_CONFIDENCE = 1.0f

        private val NUMBER = Regex("""(\d{1,2})""")
        private val PUNCTUATION = Regex("""[,.!?;:]""")
        private val WHITESPACE = Regex("""\s+""")
        private val SUPPORTED_WAKE = Regex("""^(?:viva|vivi)\s+ơi(?:\s+|$)""")
        private val UNSUPPORTED_WAKE = Regex("""^(?:siri|alexa|hey google)\s+ơi?(?:\s+|$)""")
        private val ORDER_ID = Regex("""\b([a-z])\s*((?:\d\s*){1,6})\b""")
        private val DELIVERY_STATUS_CUES = listOf("thế nào", "trạng thái", "đến đâu")
        private val REMOVED_COMMANDS = listOf(
            Regex("""\b(?:bật|tắt)\s+(?:điều hòa|ac)\b"""),
            Regex("""đặt\s+âm lượng"""),
            Regex("""\b(?:bài trước|quay lại bài trước)\b"""),
            Regex("""\b(?:dtc|mã lỗi|xe có lỗi)\b"""),
        )
        private val TEMPERATURE_CUES = listOf("đặt", "hạ", "tăng", "giảm", "xuống", "lên", "độ")
    }
}
