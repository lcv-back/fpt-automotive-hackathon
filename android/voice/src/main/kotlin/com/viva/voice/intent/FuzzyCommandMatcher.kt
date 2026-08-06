package com.viva.voice.intent

import com.viva.voice.text.PhoneticKey
import com.viva.voice.text.VietnameseText

/**
 * Tầng cứu lệnh khi [GrammarIntentRouter] trượt vì ASR nghe sai từ.
 *
 * ## Vì sao tồn tại
 *
 * Đo trên máy 05/08, Vosk `vn-0.4` trả về cho câu *"tăng nhiệt độ lên hai mươi
 * tư độ"*:
 *
 * ```
 * "chẳng nhiệt độ lên hà my tư đồ"
 * "bây giờ từ muốn nhiệt độ tăng lên hạ vi từ độ"
 * ```
 *
 * Model **không hỏng hoàn toàn** — nó đọc đúng `nhiệt độ`, `tăng`, `lên`, `độ`.
 * Nhưng router so khớp theo chuỗi nên trượt sạch, và tài xế nhận câu từ chối.
 *
 * Việc thật cần làm không phải *"phiên âm đúng"* mà là **phân loại đúng vào 10
 * intent**. Lớp này chấm điểm câu đã nhận được với từng nhóm từ khoá của các
 * intent, so theo **âm** chứ không theo chữ.
 *
 * ## Nó KHÔNG thay [GrammarIntentRouter]
 *
 * Router vẫn chạy trước và vẫn là T0 tất định: câu nói đúng thì kết quả không
 * phụ thuộc ngưỡng nào cả. Lớp này chỉ chạy khi router đã bó tay, nên nó không
 * thể làm hỏng những ca đang đúng.
 *
 * ## Ngưỡng theo từng intent — phần an toàn
 *
 * Khớp mờ **làm tăng false accept**, và với `door_lock` thì đó là lỗi an toàn
 * chứ không phải lỗi tiện dụng. Nên mỗi intent có ngưỡng riêng: mở/khoá cửa và
 * xác nhận giao hàng đòi điểm cao hơn hẳn lệnh điều hòa. Dưới ngưỡng thì trả
 * `null` để câu rơi về `unknown` → `Deny:G3_UNSUPPORTED`, đúng đường đã có.
 */
class FuzzyCommandMatcher {

    /**
     * @param intent tên intent theo `03-contracts.md` §3
     * @param keywords các từ **phải** nghe ra được; điểm là tỉ lệ khớp
     * @param minScore ngưỡng riêng, xem ghi chú an toàn ở đầu lớp
     */
    private data class Template(
        val intent: String,
        val keywords: List<String>,
        val minScore: Double,
        /** Từ mà nếu xuất hiện thì template này KHÔNG được khớp. */
        val excludes: List<String> = emptyList(),
        val slots: (List<String>) -> Map<String, Any> = { emptyMap() },
    )

    fun match(utterance: String): Intent? {
        val tokens = VietnameseText.fold(utterance)
            .split(" ")
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val best = TEMPLATES
            .map { template -> template to score(template, tokens) }
            .filter { (template, score) -> score >= template.minScore }
            .maxByOrNull { (_, score) -> score }
            ?: return null

        val (template, score) = best
        return Intent(
            name = template.intent,
            slots = template.slots(tokens),
            // Confidence phản ánh đúng độ chắc chắn của phép khớp, không phải
            // 1.0 như tầng grammar. `G3_LOW_CONFIDENCE` cần con số thật này.
            confidence = score.toFloat(),
            tier = Intent.Tier.T0,
        )
    }

    /**
     * Tỉ lệ từ khoá nghe ra được, so theo âm.
     *
     * Template **một từ khoá** đòi khớp **trùng khít**, không được sai ký tự
     * nào. Lý do đo được ngày 06/08: template `vehicle_status_fuel` chỉ có từ
     * `xăng`, và `súng` rút khoá âm ra `xung` — lệch đúng một ký tự so với
     * `xang`, nên nó khớp. Hai lượt nói thật bị đẩy thành truy vấn xăng:
     *
     * ```
     * "thành nổ súng máy biết tự động"      -> vehicle_status_fuel | Allow
     * "giảm nhiệt lũ quét súng hơi hay"     -> vehicle_status_fuel | Allow
     * ```
     *
     * Với template nhiều từ khoá, sai một ký tự ở một từ vẫn còn các từ khác
     * làm chứng. Với template một từ thì không có gì đỡ — một cú khớp yếu là
     * đủ 100% điểm. Nên chỗ này không có biên độ nào cả.
     */
    private fun score(template: Template, tokens: List<String>): Double {
        val blocked = template.excludes.any { excluded ->
            tokens.any { token -> PhoneticKey.matchesStrict(token, excluded) }
        }
        if (blocked) return 0.0

        val exactOnly = template.keywords.size == 1
        val hit = template.keywords.count { keyword ->
            tokens.any { token ->
                if (exactOnly) {
                    PhoneticKey.of(token) == PhoneticKey.of(keyword)
                } else {
                    PhoneticKey.matchesStrict(token, keyword)
                }
            }
        }
        return hit.toDouble() / template.keywords.size
    }

    private companion object {

        /**
         * Ngưỡng cao cho lệnh **không đảo ngược được từ ghế lái**.
         *
         * Mở cửa nhầm lúc xe chạy, hay đánh dấu giao xong một đơn chưa giao,
         * đều là thứ không sửa lại được bằng một câu nói khác.
         */
        const val SAFETY_CRITICAL = 1.0

        /** Lệnh tiện nghi: đoán sai thì tài xế nói lại, không mất gì. */
        const val COMFORT = 0.67

        /**
         * Từ khoá viết **có dấu** cho người đọc; [PhoneticKey] tự bỏ dấu và
         * gộp âm khi so.
         */
        val TEMPLATES = listOf(
            Template("hvac_set_temp", listOf("nhiệt", "độ"), COMFORT) { tokens ->
                numberSlot(tokens)?.let { mapOf("value" to it.toFloat()) }.orEmpty()
            },
            Template("hvac_set_temp", listOf("điều", "hòa", "độ"), COMFORT) { tokens ->
                numberSlot(tokens)?.let { mapOf("value" to it.toFloat()) }.orEmpty()
            },
            Template("hvac_set_fan", listOf("quạt", "mức"), COMFORT) { tokens ->
                numberSlot(tokens)?.let { mapOf("level" to it) }.orEmpty()
            },
            Template("door_lock", listOf("khóa", "cửa"), SAFETY_CRITICAL) {
                mapOf("lock" to true)
            },
            Template("door_lock", listOf("mở", "cửa"), SAFETY_CRITICAL) {
                mapOf("lock" to false)
            },
            Template("volume_adjust", listOf("tăng", "âm", "lượng"), COMFORT) {
                mapOf("delta" to 1)
            },
            Template("volume_adjust", listOf("giảm", "âm", "lượng"), COMFORT) {
                mapOf("delta" to -1)
            },
            Template("media_next", listOf("chuyển", "bài"), COMFORT),
            Template("media_pause", listOf("dừng", "nhạc"), COMFORT),
            Template("delivery_next_stop", listOf("chặng", "tiếp", "theo"), COMFORT),
            Template("delivery_confirm", listOf("xác", "nhận", "giao"), SAFETY_CRITICAL),
            // Truy vấn trạng thái chỉ ĐỌC, không ghi gì xuống xe, nên ngưỡng
            // thấp là hợp lý: đoán sai thì tài xế nghe một con số không hỏi,
            // chứ xe không làm gì cả.
            // Cùng lý do như ở router: "tốc độ quạt" là mức quạt. Template
            // này chỉ được khớp khi câu KHÔNG nhắc tới quạt — xem `excludes`.
            Template("vehicle_status_speed", listOf("tốc", "độ"), COMFORT, excludes = listOf("quạt")),
            Template("vehicle_status_fuel", listOf("xăng"), COMFORT),
            Template("vehicle_status_battery", listOf("pin"), COMFORT),
        )

        /**
         * Nhặt số ra khỏi câu đã méo, so **theo âm** với từ chỉ số.
         *
         * `hà my tư` → `hai mươi tư` → 24. Không có bước này thì lệnh vẫn được
         * phân loại đúng intent nhưng thiếu giá trị, và tài xế bị hỏi lại —
         * tức chỉ đỡ được một nửa.
         */
        fun numberSlot(tokens: List<String>): Int? {
            // Giữ nguyên vị trí: cần biết token nào KHÔNG phải số để vá bước sau.
            val aligned = tokens.map { token ->
                NUMBER_WORDS.firstOrNull { word -> PhoneticKey.matches(token, word) }
            }.toMutableList()

            // Vá "mươi" bị nghe hỏng ở giữa hai chữ số.
            //
            // "hai mươi tư" ra "hà my tư": `hà`≈`hai` và `tư` khớp, nhưng `my`
            // cách `mươi` quá xa để khớp theo âm. Trong tiếng Việt, hai chữ số
            // đứng cạnh nhau mà giữa có đúng một từ lạ thì từ đó gần như chắc
            // chắn là "mươi" — không có cách đọc nào khác. Vá đúng khe này chứ
            // không nới ngưỡng chung, vì nới ngưỡng là mở cửa cho false accept.
            for (i in 1 until aligned.size - 1) {
                if (aligned[i] == null && aligned[i - 1] != null && aligned[i + 1] != null) {
                    aligned[i] = "mươi"
                }
            }

            val repaired = aligned.filterNotNull()
            if (repaired.isEmpty()) return null
            return com.viva.voice.text.SpokenNumberParser
                .parse(repaired.joinToString(" "))
                ?.toInt()
        }

        val NUMBER_WORDS = listOf(
            "không", "một", "mốt", "hai", "ba", "bốn", "tư", "năm", "lăm",
            "sáu", "bảy", "tám", "chín", "mười", "mươi",
        )
    }
}
