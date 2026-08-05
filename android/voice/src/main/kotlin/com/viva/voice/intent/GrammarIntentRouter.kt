package com.viva.voice.intent

import com.viva.voice.text.SpokenNumberParser
import com.viva.voice.text.VietnameseText
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
        if (UNSUPPORTED_WAKE.containsMatchIn(fold(normalized))) {
            return RouteResult.Unsupported(
                "Từ gọi của trợ lý là “Viva ơi” hoặc “Vivi ơi”. Bạn thử lại nhé.",
                canFallback = false,
            )
        }

        // Hai bản của cùng một câu, dùng vào hai việc khác nhau:
        //  · `spoken` giữ nguyên dấu — dùng để lấy slot (tên bài hát, mã đơn),
        //    vì trả về "nhac tru tinh" thay vì "nhạc trữ tình" là làm hỏng dữ liệu.
        //  · `command` bỏ dấu — dùng cho MỌI phép so khớp.
        //
        // Vì sao bỏ dấu: Vosk `vn-0.4` là model nhỏ, nó thường bắt đúng âm tiết
        // nhưng sai thanh điệu — "mở cửa" ra "mơ cưa", "khóa cửa" ra "khoa cua".
        // Router cũ so khớp từng chữ nên sai một dấu là trượt sạch, rơi xuống
        // embedding rồi thành Unknown: ASR chỉ lệch một chút mà tài xế nhận
        // nguyên câu từ chối. Bỏ dấu hai vế cứu đúng nhóm lỗi đó.
        val spoken = normalized.replaceFirst(SUPPORTED_WAKE_SPOKEN, "").trim()
        val command = fold(normalized).replaceFirst(SUPPORTED_WAKE, "").trim()
        if (command.isEmpty()) {
            return RouteResult.NeedsClarification("Bạn muốn mình thực hiện việc gì?")
        }
        if (isRemovedCommand(command)) {
            return RouteResult.Unsupported(
                promptVi = "Lệnh này chưa hỗ trợ trong bản demo. Bạn thử một lệnh điều hòa, cửa, âm thanh hoặc giao hàng nhé.",
                canFallback = false,
            )
        }

        if (command.has("lạnh quá")) {
            return RouteResult.NeedsClarification(
                "Bạn muốn tăng nhiệt độ điều hòa lên bao nhiêu độ?",
            )
        }
        if (command.has("nóng quá")) {
            return RouteResult.NeedsClarification(
                "Bạn muốn giảm nhiệt độ điều hòa xuống bao nhiêu độ?",
            )
        }

        // Truy vấn trạng thái phải xét TRƯỚC lệnh đặt nhiệt độ.
        //
        // "nhiệt độ hiện tại" mà để rơi vào nhánh đặt nhiệt độ thì router đi
        // tìm một con số không tồn tại rồi hỏi lại — tài xế hỏi một câu và bị
        // hỏi ngược. Phân biệt bằng chỗ có số hay không: hỏi thì không có số.
        if (command.has("tốc độ") && !command.has("đặt")) {
            return matched("vehicle_status_speed")
        }
        if (command.has("xăng", "nhiên liệu")) {
            return matched("vehicle_status_fuel")
        }
        if (command.has("pin", "ắc quy")) {
            return matched("vehicle_status_battery")
        }
        if (command.has("nhiệt độ") && spokenOrDigitNumber(command) == null &&
            command.has(*STATUS_CUES)
        ) {
            return matched("vehicle_status_temperature")
        }

        if (isTemperatureCommand(command)) {
            return routeTemperature(command)
        }
        if (command.has("quạt")) {
            return routeFan(command)
        }
        if (command.has("mở cửa", "mở khóa cửa")) {
            return matched("door_lock", mapOf("lock" to false))
        }
        if (command.has("khóa cửa")) {
            return matched("door_lock", mapOf("lock" to true))
        }
        if (command.has("tăng âm lượng")) {
            return matched("volume_adjust", mapOf("delta" to 1))
        }
        if (command.has("giảm âm lượng")) {
            return matched("volume_adjust", mapOf("delta" to -1))
        }
        if (command.has("dừng nhạc", "tạm dừng nhạc")) {
            return matched("media_pause")
        }
        if (command.has("chuyển bài", "bài tiếp theo")) {
            return matched("media_next")
        }
        if (command.startsWith(fold("phát nhạc")) || command.startsWith(fold("phát playlist"))) {
            // Slot lấy từ `spoken`, không lấy từ bản bỏ dấu: trả về tên bài hát
            // mất dấu là làm hỏng dữ liệu chứ không phải chuẩn hoá nó.
            val query = spoken.removePrefix("phát ").takeUnless { it == "nhạc" }
            return matched("media_play", query?.let { mapOf("query" to it) }.orEmpty())
        }
        if (command.has("chặng tiếp theo", "điểm dừng tiếp theo")) {
            return matched("delivery_next_stop")
        }
        if (command.has("đơn") && command.has(*DELIVERY_STATUS_CUES)) {
            return matched("delivery_order_status", orderIdSlot(command))
        }
        if (command.has("xác nhận") && command.has("giao")) {
            return matched("delivery_confirm", orderIdSlot(command))
        }
        // Extension rule được viết dựa trên câu CÓ dấu, nên nhận `spoken`.
        extensionRules.forEach { rule ->
            rule.route(spoken)?.let { return it }
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
        val value = spokenOrDigitNumber(command)
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
        if (command.has("nhiệt độ")) return true
        if (!command.has("điều hòa")) return false
        return spokenOrDigitNumber(command) != null || command.has(*TEMPERATURE_CUES)
    }

    /**
     * Lấy số từ câu lệnh, chấp nhận **cả hai** dạng.
     *
     * Vosk `vn-0.4` không có token chữ số nào (19.529 từ, 0 token `\d+` —
     * kiểm trên chính `model-vi/graph/words.txt` ngày 05/08), nên câu nói ra
     * luôn là *"hai mươi bốn"* chứ không bao giờ là *"24"*. Chỉ tìm chữ số thì
     * mọi lệnh nhiệt độ và quạt **nói bằng miệng** đều rơi vào "hỏi lại", kể cả
     * khi ASR nghe đúng từng chữ. Đường bơm text thì lại đưa vào "24", nên lỗi
     * này không lộ ra trong benchmark — chỉ lộ khi có người nói thật.
     *
     * Chữ số vẫn được ưu tiên: nó không mơ hồ.
     */
    private fun spokenOrDigitNumber(command: String): Int? =
        SpokenNumberParser.parse(command)?.let { parsed ->
            val rounded = parsed.toInt()
            if (parsed == rounded.toDouble()) rounded else null
        }

    private fun routeFan(command: String): RouteResult {
        val level = spokenOrDigitNumber(command)
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

    /**
     * So khớp bỏ dấu. Literal trong mã vẫn viết có dấu cho người đọc; việc bỏ
     * dấu xảy ra ở đây, đúng một chỗ.
     */
    private fun String.has(vararg phrases: String): Boolean =
        phrases.any { phrase -> contains(fold(phrase)) }

    private fun normalize(raw: String): String = raw
        .lowercase(Locale.ROOT)
        .replace(PUNCTUATION, " ")
        .replace(WHITESPACE, " ")
        .trim()

    companion object {
        /**
         * Bỏ dấu tiếng Việt: tách tổ hợp bằng NFD rồi xoá dấu phụ, và `đ` → `d`
         * (NFD không tách chữ này).
         *
         * Chỉ chạm vào chữ cái, nên áp lên **mẫu regex** cũng an toàn: mọi ký
         * tự điều khiển của regex đi qua nguyên vẹn.
         */
        internal fun fold(raw: String): String = VietnameseText.fold(raw)

        private fun foldedRegex(pattern: String) = Regex(fold(pattern))

        private const val MIN_TEMPERATURE_C = 16
        private const val MAX_TEMPERATURE_C = 32
        private const val MIN_FAN_LEVEL = 0
        private const val MAX_FAN_LEVEL = 5
        private const val GRAMMAR_CONFIDENCE = 1.0f

        private val PUNCTUATION = Regex("""[,.!?;:]""")
        private val WHITESPACE = Regex("""\s+""")
        /** Dùng trên câu đã bỏ dấu. */
        private val SUPPORTED_WAKE = foldedRegex("""^(?:viva|vivi)\s+ơi(?:\s+|$)""")
        /** Cùng mẫu nhưng dùng trên câu còn dấu, để cắt tiền tố khỏi `spoken`. */
        private val SUPPORTED_WAKE_SPOKEN = Regex("""^(?:viva|vivi)\s+ơi(?:\s+|$)""")
        private val UNSUPPORTED_WAKE = foldedRegex("""^(?:siri|alexa|hey google)\s+ơi?(?:\s+|$)""")
        private val ORDER_ID = Regex("""\b([a-z]\d{1,6})\b""")
        private val DELIVERY_STATUS_CUES = arrayOf("thế nào", "trạng thái", "đến đâu")
        private val REMOVED_COMMANDS = listOf(
            foldedRegex("""\b(?:bật|tắt)\s+(?:điều hòa|ac)\b"""),
            foldedRegex("""đặt\s+âm lượng"""),
            foldedRegex("""\b(?:bài trước|quay lại bài trước)\b"""),
            foldedRegex("""\b(?:dtc|mã lỗi|xe có lỗi)\b"""),
        )
        /** Dấu hiệu câu HỎI, dùng để tách truy vấn khỏi lệnh đặt giá trị. */
        private val STATUS_CUES = arrayOf(
            "bao nhiêu", "hiện tại", "mấy", "thế nào", "còn", "đang",
        )

        private val TEMPERATURE_CUES = arrayOf("đặt", "hạ", "tăng", "giảm", "xuống", "lên", "độ")
    }
}
