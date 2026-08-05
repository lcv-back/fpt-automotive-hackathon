package com.sopa.viva_automotive.feature.voice.domain.audio

/**
 * Chỉnh âm lượng phát nhạc và **đọc lại** để biết nó có nhúc nhích thật không.
 *
 * Đọc lại là điểm mấu chốt, không phải chi tiết thừa: `adjustStreamVolume` là
 * lệnh một chiều, không báo thành công hay thất bại. Nếu chỉ gọi rồi nói *"đã
 * tăng âm lượng"* thì câu đó đúng hay sai đội mình cũng không biết — đúng loại
 * lời khai không kiểm chứng được mà `18-CLAIM-EVIDENCE-MAP` cấm.
 */
interface VolumeController {

    /**
     * [delta] dương là tăng, âm là giảm; độ lớn không dùng tới vì hệ thống
     * chỉnh theo từng nấc.
     *
     * Trả về câu tiếng Việt cho tài xế khi âm lượng **thật sự** đổi, hoặc khi
     * nó đã ở biên (đã to nhất/nhỏ nhất — vẫn là trả lời đúng). Trả `failure`
     * khi ra lệnh mà giá trị không đổi và cũng không ở biên: lúc đó có thứ khác
     * đang giữ âm lượng, và tài xế cần biết điều đó thay vì nghe một câu xác
     * nhận sai.
     */
    suspend fun adjust(delta: Int): Result<String>
}

/**
 * Phần quyết định của [VolumeController], tách khỏi `AudioManager` để test được
 * trên JVM — chạm vào framework Android trong unit test là dính
 * *"not mocked"*, và đây mới là phần dễ sai.
 */
object VolumeOutcome {

    sealed interface Result {
        /** Âm lượng đổi thật, hoặc đã ở biên — câu trả lời cho tài xế. */
        data class Spoken(val messageVi: String) : Result

        /** Không chỉnh được; [reasonVi] nói rõ vì sao, không nói chung chung. */
        data class Refused(val reasonVi: String) : Result
    }

    /**
     * @param volumeFixed nền tảng tự khai không cho app chỉnh âm lượng
     * @param before giá trị đọc trước khi ra lệnh
     * @param after giá trị đọc lại sau khi ra lệnh; bằng [before] nếu chưa gọi
     */
    fun of(
        volumeFixed: Boolean,
        delta: Int,
        before: Int,
        after: Int,
        max: Int,
        min: Int,
    ): Result = when {
        // Kiểm TRƯỚC nhánh "đã ở mức cao nhất": trên AAOS, âm lượng do
        // CarAudioService giữ và AudioManager báo cứng ở max. Nếu không chặn ở
        // đây thì "tăng âm lượng" lúc đang ở đỉnh sẽ trả lời "đã ở mức cao nhất
        // rồi" và thành Allow trong benchmark — bảng xanh cho một tính năng
        // không điều khiển được gì.
        volumeFixed -> Result.Refused(
            "Nền tảng này không cho ứng dụng chỉnh âm lượng; âm lượng do " +
                "CarAudioService quản lý (cần CarAudioManager + quyền privileged).",
        )

        delta > 0 && before >= max -> Result.Spoken("Âm lượng đang ở mức cao nhất rồi.")
        delta < 0 && before <= min -> Result.Spoken("Âm lượng đang ở mức thấp nhất rồi.")

        after > before -> Result.Spoken("Đã tăng âm lượng.")
        after < before -> Result.Spoken("Đã giảm âm lượng.")

        else -> Result.Refused(
            "Mình đã gửi lệnh nhưng âm lượng không đổi (vẫn ở mức $before/$max).",
        )
    }
}
