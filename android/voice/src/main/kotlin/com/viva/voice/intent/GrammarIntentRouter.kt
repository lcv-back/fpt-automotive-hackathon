package com.viva.voice.intent

import java.util.Locale

/**
 * Deterministic T0 router for the five demo command families.
 *
 * The optional wake phrase is stripped here so the same router works with a
 * future wake-word detector and with today's push-to-talk fallback.
 */
class GrammarIntentRouter : IntentRouter {

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
            val query = command.removePrefix("phát ").takeUnless { it == "nhạc" }
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
        return RouteResult.Unsupported()
    }

    private fun isRemovedCommand(command: String): Boolean =
        REMOVED_COMMANDS.any { pattern -> pattern.containsMatchIn(command) }

    private fun orderIdSlot(command: String): Map<String, Any> =
        ORDER_ID.find(command)
            ?.groupValues
            ?.get(1)
            ?.uppercase(Locale.ROOT)
            ?.let { mapOf("orderId" to it) }
            .orEmpty()

    private fun routeTemperature(command: String): RouteResult {
        val value = NUMBER.find(command)?.groupValues?.get(1)?.toIntOrNull()
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
        return NUMBER.containsMatchIn(command) || TEMPERATURE_CUES.any { cue -> command.contains(cue) }
    }

    private fun routeFan(command: String): RouteResult {
        val level = NUMBER.find(command)?.groupValues?.get(1)?.toIntOrNull()
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

    private fun normalize(raw: String): String = raw
        .lowercase(Locale.ROOT)
        .replace(PUNCTUATION, " ")
        .replace(WHITESPACE, " ")
        .trim()

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
        private val ORDER_ID = Regex("""\b([a-z]\d{1,6})\b""")
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
