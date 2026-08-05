package com.viva.voice.text

/**
 * Rút một từ tiếng Việt về **khoá âm** — dạng chuẩn hoá để so hai từ nghe
 * giống nhau nhưng viết khác.
 *
 * ## Vì sao cần
 *
 * Vosk `vn-0.4` giải mã trên 19.529 từ cho một miền 10 lệnh, nên nó thường trả
 * về từ *nghe na ná* thay vì từ đúng. Đo trên máy 05/08, cùng một câu
 * *"tăng nhiệt độ lên hai mươi tư độ"* ra hai lần khác nhau:
 *
 * ```
 * "chẳng nhiệt độ lên hà my tư đồ"
 * "bây giờ từ muốn nhiệt độ tăng lên hạ vi từ độ"
 * ```
 *
 * `tăng`→`chẳng`, `hai mươi tư`→`hà my tư`, `độ`→`đồ`. So khớp theo chữ thì
 * trượt hết; so theo âm thì `tang`/`chang` chỉ lệch một phụ âm đầu.
 *
 * ## Quy tắc gộp — cố ý giữ ít
 *
 * Chỉ gộp những cặp mà model thật sự lẫn, quan sát được từ transcript:
 *  · `ch` ~ `tr`   — `chẳng` / `trăng`
 *  · `d` ~ `gi` ~ `r` — ba âm này trùng nhau ở phương ngữ Bắc
 *  · `s` ~ `x`
 *  · `y` ~ `i` ở vị trí nguyên âm
 *
 * **Không** gộp `n`~`l` dù có phương ngữ lẫn: nó biến `lên` thành `nên` và
 * `năm` thành `lăm`, tức phá luôn phần đọc số. Gộp thêm cặp nào cũng phải trả
 * giá bằng false accept, nên mỗi cặp ở đây đều phải có transcript làm chứng.
 */
object PhoneticKey {

    /**
     * Khoá âm của một từ. Chuỗi trả về không phải để đọc, chỉ để so sánh.
     */
    fun of(word: String): String {
        var key = VietnameseText.fold(word)
        // Thứ tự quan trọng: cụm hai chữ trước, đơn chữ sau.
        key = key.replace("gi", "z").replace("tr", "c").replace("ch", "c")
        key = key.replace("d", "z").replace("r", "z")
        key = key.replace("s", "x")
        key = key.replace("y", "i")
        return key
    }

    /**
     * Khoảng cách sửa **chuẩn hoá theo độ dài**, trong khoảng `0.0`–`1.0`.
     *
     * Chuẩn hoá để một ngưỡng dùng được cho cả từ ngắn lẫn từ dài: sai một chữ
     * trong `mở` nghiêm trọng hơn nhiều so với sai một chữ trong `nhiệt`.
     */
    fun distance(a: String, b: String): Double {
        val ka = of(a)
        val kb = of(b)
        if (ka == kb) return 0.0
        if (ka.isEmpty() || kb.isEmpty()) return 1.0
        return editDistance(ka, kb).toDouble() / maxOf(ka.length, kb.length)
    }

    /**
     * So khớp **chặt**, dùng cho từ khoá quyết định lệnh.
     *
     * Hai điều kiện, phải thoả cả hai:
     *  · từ ngắn (≤ 3 ký tự sau khi rút khoá) phải **trùng khít**
     *  · từ dài hơn được sai **đúng một** ký tự
     *
     * Vì sao chặt tới vậy: đo ngày 05/08, ngưỡng tương đối 0.34 làm rác phòng
     * *"họp mặt sạch hóa thạch…"* khớp thành `delivery_confirm` — `xác`/`sạch`
     * và `nhận`/`nhạc` chỉ lệch một ký tự. Với một từ bốn chữ thì "lệch một" là
     * 25%, nghe có vẻ chặt, nhưng tiếng Việt có quá nhiều từ cách nhau đúng một
     * phụ âm cuối. Lệnh mở cửa mà nhận nhầm từ tiếng ồn là lỗi an toàn, nên chỗ
     * này chọn bỏ sót hơn là nhận bừa.
     */
    fun matchesStrict(a: String, b: String): Boolean {
        val ka = of(a)
        val kb = of(b)
        if (ka == kb) return true
        if (maxOf(ka.length, kb.length) <= SHORT_WORD) return false
        return editDistance(ka, kb) <= 1
    }

    /**
     * So khớp **nới**, chỉ dùng cho lớp từ đóng và không quyết định an toàn —
     * hiện chỉ có từ chỉ số.
     *
     * Nghe nhầm nhiệt độ là chuyện tiện nghi: tài xế nói lại. Nghe nhầm lệnh mở
     * cửa thì không. Ngưỡng phải phản ánh đúng chênh lệch rủi ro đó, chứ không
     * phải một con số dùng chung cho tất cả.
     */
    fun matches(a: String, b: String, tolerance: Double = DEFAULT_TOLERANCE): Boolean =
        distance(a, b) <= tolerance

    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(substitution, previous[j] + 1, current[j - 1] + 1)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    /**
     * Ngưỡng mặc định `0.34`: cho phép sai **một** ký tự trong từ ba chữ trở
     * lên, nhưng không cho `mo` khớp `mua`. Con số này được chọn từ chính các
     * transcript quan sát được, không phải bốc.
     */
    const val DEFAULT_TOLERANCE = 0.34

    /** Từ ngắn tới mức một ký tự sai là đổi hẳn nghĩa. */
    private const val SHORT_WORD = 3
}
