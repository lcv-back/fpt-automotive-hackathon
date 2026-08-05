package com.viva.voice.intent

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bộ đo độ bền của router trước **lỗi nhận dạng thật**, không phải trước câu sạch.
 *
 * ## Vì sao cần bộ này
 *
 * `GrammarIntentRouterTest` kiểm câu viết đúng chính tả. Nhưng thứ router nhận
 * trong đời thật là đầu ra của Vosk `vn-0.4` — một model **nhỏ** giải mã trên
 * toàn bộ từ điển tiếng Việt. Nhóm lỗi áp đảo của nó không phải nghe nhầm hẳn
 * từ, mà là **sai thanh điệu**: `mở cửa` → `mơ cưa`, `khóa cửa` → `khoa cua`.
 *
 * Router cũ so khớp từng chữ, nên sai một dấu là trượt sạch → rơi xuống
 * embedding → `Unknown` → tài xế nhận nguyên câu từ chối cho một lệnh mà ASR
 * gần như đã nghe đúng. Bộ test này chính là thước đo cho việc đó, và là hàng
 * rào để không ai vô tình gỡ mất khả năng chịu lỗi khi sửa router sau này.
 *
 * ## Cách đọc
 *
 * Mỗi ca là *(câu ASR có thể trả về, intent đúng)*. Biến thể được sinh theo ba
 * kiểu lỗi quan sát được ở model nhỏ tiếng Việt:
 *   1. mất hết dấu — `hạ điều hòa` → `ha dieu hoa`
 *   2. sai thanh, giữ nguyên âm — `mở cửa` → `mờ cửa`
 *   3. lẫn `d`/`đ` — `điều hòa` → `dieu hoa`
 *
 * ⚠️ Bộ này **không** đo chất lượng ASR. Nó đo *phần dưới* ASR: khi ASR đã sai
 * theo kiểu nhẹ như trên thì hệ thống còn cứu được lệnh hay không.
 */
class MisrecognitionRobustnessTest {

    private val router = GrammarIntentRouter()

    private fun intentOf(utterance: String): String =
        when (val result = router.route(utterance)) {
            is RouteResult.Matched -> result.intent.name
            is RouteResult.NeedsClarification -> "clarify"
            is RouteResult.Unsupported -> "unsupported"
        }

    /**
     * Gom hết sai lệch rồi mới báo, thay vì dừng ở ca đầu tiên.
     *
     * JUnit mặc định fail-fast, nên một nhóm 8 biến thể hỏng 5 cái vẫn chỉ hiện
     * ra 1 dòng. Khi thứ đang đo là **tỉ lệ chịu lỗi**, con số đó phải đầy đủ —
     * nếu không thì không so được trước/sau.
     */
    private fun assertRoutes(expected: String, vararg variants: String) {
        val wrong = variants.mapNotNull { variant ->
            val actual = intentOf(variant)
            if (actual == expected) null else "\"$variant\" → $actual (đúng phải là $expected)"
        }
        if (wrong.isNotEmpty()) {
            throw AssertionError(
                "${wrong.size}/${variants.size} biến thể sai:\n  " + wrong.joinToString("\n  "),
            )
        }
    }

    /**
     * Một phép đo duy nhất trên toàn corpus, để có **một con số** so được
     * trước/sau thay vì một đống test rời fail-fast.
     */
    @Test
    fun `intent accuracy on the mis-recognition corpus`() {
        val wrong = CORPUS.mapNotNull { (utterance, expected) ->
            val actual = intentOf(utterance)
            if (actual == expected) null else "\"$utterance\" → $actual (đúng: $expected)"
        }
        val correct = CORPUS.size - wrong.size
        if (wrong.isNotEmpty()) {
            throw AssertionError(
                "Độ chính xác intent: $correct/${CORPUS.size} " +
                    "(${wrong.size} sai)\n  " + wrong.joinToString("\n  "),
            )
        }
        assertEquals(CORPUS.size, correct)
    }

    @Test
    fun `door commands survive missing and wrong tone marks`() {
        assertRoutes(
            "door_lock",
            "khóa cửa",      // chuẩn
            "khoa cua",      // mất hết dấu
            "khóa cưa",      // sai thanh ở âm tiết hai
            "khoá cửa",      // dấu đặt khác vị trí, rất hay gặp
        )
        assertRoutes(
            "door_lock",
            "mở cửa",
            "mo cua",
            "mờ cửa",
            "mở cưa",
        )
    }

    @Test
    fun `hvac commands survive missing tone marks`() {
        assertRoutes(
            "hvac_set_temp",
            "hạ điều hòa xuống 24 độ",
            "ha dieu hoa xuong 24 do",
            "hạ diều hoà xuống 24 dộ",
            "đặt nhiệt độ 26 độ",
            "dat nhiet do 26 do",
        )
        assertRoutes(
            "hvac_set_fan",
            "quạt mức 3",
            "quat muc 3",
            "quát mức 3",
        )
    }

    @Test
    fun `delivery commands survive missing tone marks`() {
        assertRoutes(
            "delivery_next_stop",
            "chặng tiếp theo là gì",
            "chang tiep theo la gi",
        )
        assertRoutes(
            "delivery_confirm",
            "xác nhận giao thành công đơn a12",
            "xac nhan giao thanh cong don a12",
        )
    }

    @Test
    fun `the wake phrase still strips when its tone is lost`() {
        assertRoutes(
            "hvac_set_temp",
            "viva ơi hạ điều hòa xuống 24 độ",
            "viva oi ha dieu hoa xuong 24 do",
        )
    }

    /**
     * Hàng rào an toàn — quan trọng ngang phần trên.
     *
     * Bỏ dấu làm không gian khớp rộng ra, nên phải chứng minh nó **không** kéo
     * câu ngoài phạm vi thành lệnh xe. Nếu một ngày ai đó thêm khớp mờ quá tay,
     * test này đổ trước khi lỗi kịp ra tới xe.
     */
    @Test
    fun `folding tones does not turn out-of-scope speech into a vehicle command`() {
        assertRoutes(
            "unsupported",
            "đặt bàn ăn tối",
            "dat ban an toi",
            "kể một câu chuyện cười",
            "ke mot cau chuyen cuoi",
            "gọi cho vợ tôi",
        )
    }

    /** Wake phrase của trợ lý khác vẫn phải bị từ chối, kể cả khi mất dấu. */
    @Test
    fun `another assistant wake phrase is still refused without tone marks`() {
        assertRoutes(
            "unsupported",
            "siri ơi hạ điều hòa xuống 24 độ",
            "siri oi ha dieu hoa xuong 24 do",
        )
    }

    /** Lệnh đã cắt khỏi phạm vi vẫn phải bị chặn khi mất dấu. */
    @Test
    fun `removed commands stay removed without tone marks`() {
        assertRoutes(
            "unsupported",
            "bật điều hòa",
            "bat dieu hoa",
        )
    }

    private companion object {
        /**
         * Corpus có nhãn: *(câu ASR có thể trả về, intent đúng)*.
         *
         * Nguồn câu chuẩn là `backend/suites/benchmark_v1.csv`; biến thể sinh
         * tay theo ba kiểu lỗi của model nhỏ tiếng Việt (mất dấu / sai thanh /
         * lẫn `d`-`đ`). Đây là **dữ liệu tự tạo**, phải khai đúng như vậy —
         * nó đo độ bền của router, không đo chất lượng ASR.
         */
        val CORPUS: List<Pair<String, String>> = listOf(
            // — cửa —
            "khóa cửa" to "door_lock",
            "khoa cua" to "door_lock",
            "khóa cưa" to "door_lock",
            "khoá cửa" to "door_lock",
            "mở cửa" to "door_lock",
            "mo cua" to "door_lock",
            "mờ cửa" to "door_lock",
            "mở cưa" to "door_lock",
            // — điều hòa —
            "hạ điều hòa xuống 24 độ" to "hvac_set_temp",
            "ha dieu hoa xuong 24 do" to "hvac_set_temp",
            "hạ diều hoà xuống 24 dộ" to "hvac_set_temp",
            "đặt nhiệt độ 26 độ" to "hvac_set_temp",
            "dat nhiet do 26 do" to "hvac_set_temp",
            "quạt mức 3" to "hvac_set_fan",
            "quat muc 3" to "hvac_set_fan",
            "quát mức 3" to "hvac_set_fan",
            // — số viết bằng chữ: dạng Vosk THẬT SỰ trả về —
            //
            // `model-vi/graph/words.txt` có 19.529 từ và **0 token chữ số**
            // (kiểm 05/08). Nói "24 độ" thì thứ đi vào router là "hai mươi bốn
            // độ". Nhóm này từng hỏng 100% mà benchmark không thấy, vì đường
            // bơm text đưa vào chữ số.
            "hạ điều hòa xuống hai mươi bốn độ" to "hvac_set_temp",
            "ha dieu hoa xuong hai muoi bon do" to "hvac_set_temp",
            "đặt nhiệt độ hai mươi sáu độ" to "hvac_set_temp",
            "đặt nhiệt độ mười tám độ" to "hvac_set_temp",
            "quạt mức ba" to "hvac_set_fan",
            "quat muc ba" to "hvac_set_fan",
            "quạt mức năm" to "hvac_set_fan",
            // — âm lượng —
            "tăng âm lượng" to "volume_adjust",
            "tang am luong" to "volume_adjust",
            "giảm âm lượng" to "volume_adjust",
            "giam am luong" to "volume_adjust",
            // — giao hàng —
            "chặng tiếp theo là gì" to "delivery_next_stop",
            "chang tiep theo la gi" to "delivery_next_stop",
            "xác nhận giao thành công đơn a12" to "delivery_confirm",
            "xac nhan giao thanh cong don a12" to "delivery_confirm",
            // — từ gọi —
            "viva ơi hạ điều hòa xuống 24 độ" to "hvac_set_temp",
            "viva oi ha dieu hoa xuong 24 do" to "hvac_set_temp",
            // — hàng rào an toàn: phải KHÔNG thành lệnh xe —
            "đặt bàn ăn tối" to "unsupported",
            "dat ban an toi" to "unsupported",
            "kể một câu chuyện cười" to "unsupported",
            "gọi cho vợ tôi" to "unsupported",
            "siri ơi hạ điều hòa xuống 24 độ" to "unsupported",
            "siri oi ha dieu hoa xuong 24 do" to "unsupported",
            "bật điều hòa" to "unsupported",
            "bat dieu hoa" to "unsupported",
        )
    }
}
