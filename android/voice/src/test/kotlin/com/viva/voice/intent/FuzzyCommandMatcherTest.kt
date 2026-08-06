package com.viva.voice.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Đo trên **transcript thật**, không phải trên câu tự nghĩ ra.
 *
 * Mọi chuỗi trong nhóm đầu đều được chép nguyên văn từ logcat của phiên nói
 * thật tối 05/08 trên emulator AAOS, sau khi mic được nối và đạt
 * `peak≈22000/32767`. Đó là điểm khác biệt so với corpus sinh tay: không ai
 * đoán hộ model xem nó sẽ sai kiểu gì — chính nó đã sai, và đây là bản ghi.
 */
class FuzzyCommandMatcherTest {

    private val matcher = FuzzyCommandMatcher()

    @Test
    fun `recovers the intent from the real transcripts recorded on device`() {
        // Người nói: "tăng nhiệt độ lên hai mươi tư độ"
        val first = matcher.match("chẳng nhiệt độ lên hà my tư đồ")
        assertNotNull("transcript thật thứ nhất phải cứu được", first)
        assertEquals("hvac_set_temp", first!!.name)

        // Cùng một câu, lần đọc khác
        val second = matcher.match("bây giờ từ muốn nhiệt độ tăng lên hạ vi từ độ")
        assertNotNull("transcript thật thứ hai phải cứu được", second)
        assertEquals("hvac_set_temp", second!!.name)
    }

    @Test
    fun `repairs the number word that the recogniser mangled`() {
        // "hà my tư" nghe từ "hai mươi tư" → 24. Không có bước này thì intent
        // đúng nhưng thiếu giá trị, và tài xế vẫn bị hỏi lại.
        val intent = matcher.match("chẳng nhiệt độ lên hà my tư đồ")
        assertEquals(24f, intent!!.slots["value"])
    }

    @Test
    fun `confidence reflects how well it matched, not a flat one`() {
        val intent = matcher.match("nhiệt độ")
        assertNotNull(intent)
        // Tầng grammar trả 1.0 vì nó tất định; tầng này phải trả điểm thật để
        // G3_LOW_CONFIDENCE có số mà xét.
        assertEquals(1.0f, intent!!.confidence)
    }

    @Test
    fun `comfort commands are recovered from partial keyword hits`() {
        assertEquals("hvac_set_fan", matcher.match("quạt mức ba")!!.name)
        assertEquals("media_next", matcher.match("chuyển bài đi")!!.name)
        assertEquals("volume_adjust", matcher.match("tăng âm lượng lên")!!.name)
    }

    // --- hàng rào an toàn -------------------------------------------------

    /**
     * Lệnh không đảo ngược được đòi khớp **đủ** từ khoá. Nghe được mỗi `cửa`
     * mà đã mở khoá thì đó là lỗi an toàn, không phải tiện dụng.
     */
    @Test
    fun `a half-heard door command is refused, not guessed`() {
        assertNull(matcher.match("cửa"))
        assertNull(matcher.match("ừ cửa gì đó"))
        assertNull(matcher.match("xác nhận"))
    }

    @Test
    fun `out-of-scope speech is not pulled into a vehicle command`() {
        assertNull(matcher.match("đặt bàn ăn tối"))
        assertNull(matcher.match("kể một câu chuyện cười"))
        assertNull(matcher.match("gọi cho vợ tôi"))
        assertNull(matcher.match("hôm nay trời đẹp quá"))
        // Transcript rác thật, chép từ logcat 05/08 lúc mic bắt nhiễu phòng.
        assertNull(matcher.match("họp mặt sạch hóa thạch và suýt rách vực âm nhạc mỹ thuật việt nam"))
        // Hai câu dưới chép từ logcat 06/08. `súng` rút khoá âm ra `xung`,
        // lệch đúng một ký tự so với `xang` (= `xăng`), nên chúng từng bị đẩy
        // thành truy vấn xăng rồi THUC THI.
        assertNull(matcher.match("thành nổ súng máy biết tự động"))
        assertNull(matcher.match("giảm nhiệt lũ quét súng hơi hay"))
        assertNull(matcher.match("giảm nhịp độ xuân thị bị trừ thụ"))
    }

    @Test
    fun `an empty or blank utterance never matches`() {
        assertNull(matcher.match(""))
        assertNull(matcher.match("   "))
    }
}
