package com.sopa.viva_automotive.feature.voice.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeOutcomeTest {

    /**
     * Ca quan trọng nhất, và là ca đã suýt lọt: trên emulator AAOS, âm lượng do
     * CarAudioService giữ, `AudioManager` báo cứng 15/15 và `isVolumeFixed` là
     * true. Nếu xét biên trước thì "tăng âm lượng" sẽ trả lời *"đã ở mức cao
     * nhất rồi"* — nghe hợp lý, nhưng thành `Allow` trong benchmark, tức bảng
     * kết quả xanh cho một tính năng không điều khiển được gì.
     */
    @Test
    fun `a platform that fixes volume is refused even when already at the top`() {
        val outcome = VolumeOutcome.of(
            volumeFixed = true,
            delta = 1,
            before = 15,
            after = 15,
            max = 15,
            min = 0,
        )

        assertTrue("$outcome", outcome is VolumeOutcome.Result.Refused)
        assertTrue(
            (outcome as VolumeOutcome.Result.Refused).reasonVi,
            outcome.reasonVi.contains("CarAudioService"),
        )
    }

    @Test
    fun `a real increase is reported as one`() {
        val outcome = VolumeOutcome.of(false, delta = 1, before = 7, after = 8, max = 15, min = 0)

        assertEquals(VolumeOutcome.Result.Spoken("Đã tăng âm lượng."), outcome)
    }

    @Test
    fun `a real decrease is reported as one`() {
        val outcome = VolumeOutcome.of(false, delta = -1, before = 7, after = 6, max = 15, min = 0)

        assertEquals(VolumeOutcome.Result.Spoken("Đã giảm âm lượng."), outcome)
    }

    @Test
    fun `being at the boundary is a truthful answer, not a failure`() {
        assertEquals(
            VolumeOutcome.Result.Spoken("Âm lượng đang ở mức cao nhất rồi."),
            VolumeOutcome.of(false, delta = 1, before = 15, after = 15, max = 15, min = 0),
        )
        assertEquals(
            VolumeOutcome.Result.Spoken("Âm lượng đang ở mức thấp nhất rồi."),
            VolumeOutcome.of(false, delta = -1, before = 0, after = 0, max = 15, min = 0),
        )
    }

    @Test
    fun `a command that moved nothing is refused, not quietly reported as done`() {
        // Không ở biên, nền tảng bảo chỉnh được, mà giá trị vẫn y nguyên: có thứ
        // khác đang giữ âm lượng. Tài xế cần biết, thay vì nghe "đã tăng".
        val outcome = VolumeOutcome.of(false, delta = 1, before = 7, after = 7, max = 15, min = 0)

        assertTrue("$outcome", outcome is VolumeOutcome.Result.Refused)
        assertTrue(
            (outcome as VolumeOutcome.Result.Refused).reasonVi,
            outcome.reasonVi.contains("7/15"),
        )
    }
}
