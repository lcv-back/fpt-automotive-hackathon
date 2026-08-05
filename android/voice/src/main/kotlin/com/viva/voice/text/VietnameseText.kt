package com.viva.voice.text

import java.text.Normalizer

/**
 * Chuẩn hoá tiếng Việt dùng chung cho tầng NLU.
 *
 * Đặt ở một chỗ vì cả router lẫn bộ đọc số đều cần **cùng một** phép bỏ dấu.
 * Hai bản sao lệch nhau là cách chắc chắn để "mở cửa" khớp được mà
 * "hai mươi bốn" thì không.
 */
object VietnameseText {

    private val COMBINING_MARKS = Regex("""\p{Mn}+""")

    /**
     * Bỏ dấu: tách tổ hợp bằng NFD rồi xoá dấu phụ, và `đ` → `d` (NFD không
     * tách chữ này).
     *
     * Chỉ chạm vào chữ cái, nên áp lên **mẫu regex** cũng an toàn: mọi ký tự
     * điều khiển của regex đi qua nguyên vẹn.
     */
    fun fold(raw: String): String =
        Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace("đ", "d")
}
