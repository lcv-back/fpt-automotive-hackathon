package com.viva.voice.intent

import com.viva.voice.text.VietnameseText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Giữ [CommandVocabulary] và [GrammarIntentRouter] không trôi khỏi nhau.
 *
 * Ràng buộc vốn từ cho Vosk chỉ có tác dụng nếu vốn từ đó **phủ được** mọi câu
 * mà router biết xử lý. Thêm một luật vào router mà quên thêm từ thì bộ nhận
 * dạng vĩnh viễn không đọc nổi lệnh đó — và lỗi ấy chỉ lộ ra khi có người nói
 * thật, tức lúc quay demo. Test này bắt nó ngay khi biên dịch.
 */
class CommandVocabularyTest {

    private val vocabulary = CommandVocabulary.words.map(VietnameseText::fold).toSet()

    /**
     * Corpus lệnh chuẩn, lấy từ `backend/suites/benchmark_v1.csv` và các câu
     * `GrammarIntentRouterTest` đang khẳng định.
     */
    private val commands = listOf(
        "viva ơi hạ điều hòa xuống hai mươi bốn độ",
        "hạ điều hòa xuống hai mươi hai độ",
        "đặt nhiệt độ hai mươi sáu độ",
        "quạt mức ba",
        "quạt mức năm",
        "khóa cửa",
        "mở cửa",
        "mở khóa cửa",
        "tăng âm lượng",
        "giảm âm lượng",
        "phát nhạc",
        "chuyển bài",
        "dừng nhạc",
        "chặng tiếp theo là gì",
        "đơn a12 thế nào",
        "xác nhận giao thành công đơn a12",
        "lạnh quá",
        "nóng quá",
    )

    @Test
    fun `every word the router understands is in the recogniser vocabulary`() {
        val missing = commands
            .flatMap { command -> command.split(" ") }
            .map(VietnameseText::fold)
            .filter { word -> word.isNotBlank() }
            // Mã đơn ("a12") do người nói đánh vần, không thuộc vốn từ cố định.
            .filterNot { word -> word.any(Char::isDigit) }
            .distinct()
            .filterNot { word -> word in vocabulary }

        assertTrue(
            "Thiếu ${missing.size} từ trong CommandVocabulary: $missing\n" +
                "Thêm luật vào router thì phải thêm từ vào đây, nếu không Vosk " +
                "không bao giờ đọc nổi lệnh đó.",
            missing.isEmpty(),
        )
    }

    /**
     * Không có `[unk]` thì câu ngoài miền bị **ép** về tổ hợp từ gần nhất trong
     * danh sách — *"đặt bàn ăn tối"* có thể thành một lệnh xe thật. Đó là lỗi
     * an toàn, không phải lỗi chính tả, nên nó có test riêng.
     */
    @Test
    fun `the unknown token is always offered to the decoder`() {
        assertTrue(CommandVocabulary.asVoskGrammar().contains("\"[unk]\""))
    }

    @Test
    fun `the grammar is a well formed json array of quoted words`() {
        val grammar = CommandVocabulary.asVoskGrammar()
        assertTrue(grammar.startsWith("[") && grammar.endsWith("]"))
        assertEquals(
            CommandVocabulary.words.size + 1,
            grammar.count { it == ',' } + 1,
        )
    }

    /**
     * `vivi` **cố ý** không có mặt: đối chiếu ngày 05/08 với
     * `model-vi/graph/words.txt` (19.529 từ) cho thấy model không có từ này,
     * và một từ ngoài từ điển làm Vosk từ chối cả grammar. Ngữ pháp vẫn chấp
     * nhận *"Vivi ơi"* cho người gõ tay hoặc engine khác — chỉ Vosk là không
     * bao giờ nghe ra nó.
     */
    @Test
    fun `vivi is deliberately absent because the model lexicon lacks it`() {
        assertTrue("vivi" !in CommandVocabulary.words)
        assertTrue("viva" in CommandVocabulary.words)
    }
}
