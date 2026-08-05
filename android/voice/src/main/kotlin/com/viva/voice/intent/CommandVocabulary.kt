package com.viva.voice.intent

/**
 * Vốn từ mà bộ nhận dạng được phép xuất ra.
 *
 * ## Vì sao cần
 *
 * Vosk `vn-0.4` là model **nhỏ** nhưng giải mã trên **19.529 từ** — toàn bộ
 * tiếng Việt — cho một miền chỉ có 10 lệnh. Đo trên máy thật ngày 05/08, câu
 * *"tăng nhiệt độ lên hai mươi tư độ"* ra:
 *
 * ```
 * "chẳng nhiệt độ lên hà my tư đồ"
 * ```
 *
 * Đúng cấu trúc, nhưng `tăng`→`chẳng`, `hai mươi tư`→`hà my tư`, `độ`→`đồ`.
 * Bộ giải mã có 19.529 lựa chọn cho mỗi từ, nên nó lạc sang những từ nghe
 * na ná mà miền lệnh không bao giờ dùng tới.
 *
 * Truyền danh sách này vào `Recognizer(model, rate, grammar)` thu không gian
 * tìm kiếm về đúng vốn từ của 10 intent. Đây là cách chặn lỗi **ở nguồn**,
 * khác với việc vá ở tầng router phía dưới.
 *
 * ## `[unk]` — phần không được quên
 *
 * Ràng buộc từ vựng có một mặt trái nguy hiểm: câu **ngoài miền** sẽ bị ép về
 * tổ hợp từ gần nhất trong danh sách. Nói *"đặt bàn ăn tối"* mà máy nghe thành
 * *"đặt bốn ăn tối"* rồi thành lệnh nhiệt độ thì đó là lỗi an toàn, không phải
 * lỗi chính tả. `[unk]` cho bộ giải mã quyền trả về "không biết" thay vì đoán
 * bừa, và `G3_UNSUPPORTED` mới có cái để chặn.
 *
 * ## Ràng buộc bảo trì
 *
 * Mọi từ ở đây **phải có trong `model-vi/graph/words.txt`**, nếu không Vosk
 * dựng grammar thất bại. Đã đối chiếu 05/08: 76/77 từ đề xuất có mặt; từ duy
 * nhất bị loại là `vivi` — nghĩa là wake phrase *"Vivi ơi"* mà
 * [GrammarIntentRouter] chấp nhận **không bao giờ** ra được từ Vosk. Ngữ pháp
 * giữ nguyên để người gõ tay hoặc engine khác vẫn dùng được.
 *
 * `CommandVocabularyTest` khoá lại quan hệ với router: thêm luật mà quên thêm
 * từ thì test đổ.
 */
object CommandVocabulary {

    /** Token đặc biệt của Vosk cho "nghe được nhưng không thuộc vốn từ". */
    const val UNKNOWN = "[unk]"

    val words: List<String> = listOf(
        // từ gọi
        "viva", "ơi",
        // điều hòa
        "hạ", "tăng", "giảm", "đặt", "nhiệt", "độ", "điều", "hòa", "hoà",
        "xuống", "lên", "lạnh", "nóng", "quá",
        // quạt
        "quạt", "mức", "mạnh", "yếu", "số",
        // cửa
        "mở", "khóa", "khoá", "cửa",
        // âm lượng
        "âm", "lượng",
        // nhạc
        "nhạc", "phát", "bài", "chuyển", "tiếp", "theo", "dừng", "tạm",
        // giao hàng
        "chặng", "điểm", "đơn", "hàng", "giao", "xác", "nhận", "thành", "công",
        "thế", "nào", "trạng", "thái", "đến", "đâu", "là", "gì", "này",
        // số đếm — Vosk không có token chữ số nào, xem SpokenNumberParser
        "không", "một", "mốt", "hai", "ba", "bốn", "tư", "năm", "lăm",
        "sáu", "bảy", "tám", "chín", "mười", "mươi", "rưỡi",
        // từ đệm hay gặp trong câu nói tự nhiên
        "cho", "tôi", "ở", "của", "xe", "giúp", "với",
    )

    /**
     * Định dạng Vosk đòi: một mảng JSON các từ, kèm [UNKNOWN].
     *
     * Tự nối chuỗi thay vì kéo thư viện JSON vào — module này cố ý không có
     * dependency ngoài, và dữ liệu ở đây là hằng số do đội kiểm soát, không
     * phải đầu vào từ ngoài.
     */
    fun asVoskGrammar(): String =
        (words + UNKNOWN).joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
}
