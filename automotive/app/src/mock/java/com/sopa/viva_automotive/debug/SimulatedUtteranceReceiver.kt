package com.sopa.viva_automotive.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.sopa.viva_automotive.feature.voice.service.VoiceAssistantService

/**
 * Đẩy một câu cho sẵn vào đúng đường xử lý voice, không qua mic.
 *
 * ## Vì sao cần
 *
 * Bộ 22 câu `backend/suites/benchmark_v1.csv` chấm `expect_intent` và
 * `expect_verdict` — phần router → guard → skill. Trước receiver này, cách duy
 * nhất để chạy bộ đó trên máy thật là **có người đọc to 22 câu**, nên nó chưa
 * chạy lần nào và mọi số liệu vẫn đến từ fixture.
 *
 * ## Nó KHÔNG thay thế được cái gì
 *
 * Đường này bỏ qua mic, VAD và ASR, nên **không** đo được WER, độ ồn hay độ trễ
 * đầu-cuối. `vong2/25` §2 F7 đã ghi sẵn: bộ suite này vốn không đo những thứ
 * đó. Muốn có số liệu ASR thì vẫn phải nói thật vào mic.
 *
 * ## Chỉ có trong biến thể `mock`
 *
 * Cùng lý do với [VehicleDummyReceiver]: một lối vào chạy được lệnh xe từ ngoài
 * process không được có mặt trong bản phát hành. Bản thân service còn chặn thêm
 * một lớp nữa — nó bỏ qua action này nếu build không `debuggable`.
 *
 * ## Vì sao base64
 *
 * `adb shell am broadcast --es text "hạ điều hòa xuống 24 độ"` đi qua shell của
 * Windows rồi shell của Android; dấu tiếng Việt hỏng ở đó là chuyện thường, và
 * một câu hỏng dấu sẽ được ghi nhận thành *intent sai* — tức benchmark báo lỗi
 * cho một thứ không hề lỗi. Base64 chỉ có ASCII nên đi qua được cả hai.
 *
 * ```
 * adb shell am broadcast \
 *   -a com.sopa.viva_automotive.mock.UTTERANCE \
 *   --es text_b64 aOG6oSDEkWnhu4F1IGjDsmEgeHXhu5FuZyAyNCDEkeG7mQ== \
 *   -n com.sopa.viva_automotive.mock/com.sopa.viva_automotive.debug.SimulatedUtteranceReceiver
 * ```
 */
class SimulatedUtteranceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UTTERANCE) return

        val utterance = decode(intent.getStringExtra(EXTRA_TEXT_B64))
            ?: intent.getStringExtra(EXTRA_TEXT).orEmpty()

        if (utterance.isBlank()) {
            Log.w(TAG, "Bỏ qua: thiếu cả $EXTRA_TEXT_B64 lẫn $EXTRA_TEXT")
            return
        }

        Log.i(TAG, "Bơm câu: \"$utterance\"")
        context.startForegroundService(
            Intent(context, VoiceAssistantService::class.java)
                .setAction(VoiceAssistantService.ACTION_SIMULATE_UTTERANCE)
                .putExtra(VoiceAssistantService.EXTRA_UTTERANCE, utterance),
        )
    }

    /** Trả `null` khi không có tham số hoặc chuỗi không phải base64 hợp lệ. */
    private fun decode(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
        }.onFailure { error ->
            Log.w(TAG, "Không giải mã được $EXTRA_TEXT_B64: ${error.message}")
        }.getOrNull()
    }

    private companion object {
        const val ACTION_UTTERANCE = "com.sopa.viva_automotive.mock.UTTERANCE"
        const val EXTRA_TEXT_B64 = "text_b64"
        const val EXTRA_TEXT = "text"
        const val TAG = "SimulatedUtterance"
    }
}
