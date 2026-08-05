package com.sopa.viva_automotive.feature.voice.data.audio

import android.content.Context
import android.media.AudioManager
import com.sopa.viva_automotive.feature.voice.domain.audio.VolumeController
import com.sopa.viva_automotive.feature.voice.domain.audio.VolumeOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [VolumeController] dựa trên [AudioManager] của nền tảng.
 *
 * ## Vì sao `AudioManager` chứ không `CarAudioManager`
 *
 * `CarAudioManager.setGroupVolume` cần quyền `CAR_CONTROL_AUDIO_VOLUME` —
 * signature|privileged, tức phải cài app vào `/system/priv-app`, thứ đội đang
 * **chưa làm được** (`evidence/c2/m1a-debuggable-privapp-probe.txt`: shell không
 * ghi được vào đó).
 *
 * ## Đo được gì trên emulator AAOS 14 (05/08)
 *
 * `isVolumeFixed` trả **true**, và `dumpsys car_service` cho thấy âm lượng nằm
 * ở `CarVolumeGroup(0)` với gain index 32/38 trong khi `AudioManager` báo cứng
 * 15/15. Nghĩa là trên nền tảng này lớp này **không chỉnh được gì** — và nó nói
 * đúng như vậy thay vì trả lời cho có. Khi M1a mở được đường privileged thì đổi
 * sang `CarAudioManager`; phần nối từ intent tới đây đã sẵn.
 */
@Singleton
class AndroidVolumeController @Inject constructor(
    @ApplicationContext private val context: Context,
) : VolumeController {

    override suspend fun adjust(delta: Int): Result<String> {
        val audio = context.getSystemService(AudioManager::class.java)
            ?: return Result.failure(IllegalStateException("Không lấy được AudioManager"))

        val before = audio.getStreamVolume(STREAM)
        val max = audio.getStreamMaxVolume(STREAM)
        val min = runCatching { audio.getStreamMinVolume(STREAM) }.getOrDefault(0)
        val fixed = audio.isVolumeFixed

        // Chỉ ra lệnh khi có cơ hội đổi thật; ngoài ra `after` giữ bằng `before`
        // để phần quyết định thấy đúng "không có gì thay đổi".
        val after = if (!fixed && !atBoundary(delta, before, max, min)) {
            val direction = if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audio.adjustStreamVolume(STREAM, direction, 0)
            audio.getStreamVolume(STREAM)
        } else {
            before
        }

        return when (val outcome = VolumeOutcome.of(fixed, delta, before, after, max, min)) {
            is VolumeOutcome.Result.Spoken -> Result.success(outcome.messageVi)
            is VolumeOutcome.Result.Refused -> Result.failure(IllegalStateException(outcome.reasonVi))
        }
    }

    private fun atBoundary(delta: Int, before: Int, max: Int, min: Int): Boolean =
        (delta > 0 && before >= max) || (delta < 0 && before <= min)

    private companion object {
        /**
         * Luồng nhạc — cùng luồng mà `media_*` điều khiển. Không dùng
         * `STREAM_SYSTEM` hay `STREAM_RING`: tài xế nói "tăng âm lượng" giữa
         * lúc nghe nhạc thì ý là nhạc.
         */
        const val STREAM = AudioManager.STREAM_MUSIC
    }
}
