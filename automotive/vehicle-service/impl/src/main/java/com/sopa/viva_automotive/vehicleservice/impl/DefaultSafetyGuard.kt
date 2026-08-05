package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.SafetyGuard
import com.sopa.viva_automotive.vehicleservice.api.SafetyRules
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleCommandSource
import com.sopa.viva_automotive.vehicleservice.api.VehicleSafetyState
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteRequest
import com.sopa.viva_automotive.vehicleservice.api.Verdict
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bộ luật v1 của `03-contracts.md` §4.
 *
 * Nguyên tắc xuyên suốt: **thiếu dữ liệu thì không đoán**. Luật nào cần một
 * trường mà snapshot không có (`gear`, `parkingBrake`) thì luật đó không kích
 * hoạt, thay vì chạy trên giá trị mặc định — một luật an toàn dựa trên giá trị
 * bịa còn nguy hiểm hơn không có luật, vì nó tạo cảm giác đã được bảo vệ.
 *
 * ## Luật nào có, luật nào chưa — khai đủ để không ai claim thừa
 *
 * | Luật §4 | v1 này |
 * |---|---|
 * | `G1_SPEED_LOCK` | ✅ |
 * | `G1_GEAR_LOCK` | ✅ nhưng **chỉ chạy khi đọc được số** — hiện `VehicleStatus` chưa có |
 * | `G2_CONFIRM_DOOR` | ✅ |
 * | `G3_LOW_CONFIDENCE` | ✅ (chỉ khi lệnh đến từ giọng nói) |
 * | `G3_VALUE_RANGE` | ✅ — thêm ngoài §4, xem ghi chú dưới |
 * | `G1_STALE_STATE` | ❌ **chưa làm** |
 * | `G2_CONFIRM_DELIVERY` | ➡️ đã có ở `DeliverySkill`, không đi qua đường property |
 * | `G3_LLM_WHITELIST` | ➡️ không áp dụng — tầng T2 cloud LLM đã cắt từ 28/07 |
 * | `G3_MISSING_SLOT`, `G3_UNSUPPORTED` | ➡️ đã có ở `GrammarIntentRouter` (tầng NLU) |
 *
 * **Vì sao `G1_STALE_STATE` chưa làm:** nó cần so `timestampNanos` của snapshot
 * với đồng hồ hiện tại, mà đường lấy đồng hồ chưa được nối (dùng
 * `SystemClock.elapsedRealtimeNanos` thì test JVM không chạy được, dùng
 * `System.nanoTime` thì không cùng gốc thời gian với snapshot). Một phép kiểm
 * "cũ hay mới" chạy trên hai đồng hồ khác gốc sẽ **luôn** cho kết quả sai ở một
 * phía, và sai kiểu đó tệ hơn là không kiểm. Để lại đúng như vậy cho tới khi có
 * `VivaCarService` cấp snapshot kèm mốc thời gian cùng gốc.
 *
 * **`G3_VALUE_RANGE` không có trong §4** — thêm vào vì ablation A4 hôm 04/08 cho
 * thấy khi bỏ tầng grammar thì câu *"đặt nhiệt độ 40 độ"* đi thẳng thành
 * `SetTemperature(40.0)`. Miền giá trị hợp lệ không nên chỉ được giữ bởi một
 * tầng; đây là lớp chắn thứ hai ngay trước khi ghi xuống xe.
 */
@Singleton
class DefaultSafetyGuard @Inject constructor() : SafetyGuard {

    override fun evaluate(request: VehicleWriteRequest, state: VehicleSafetyState): Verdict {
        // Thứ tự ba tầng, và mỗi tầng có lý do:
        //
        //  1. TỪ CHỐI trước. Lệnh vừa nguy hiểm vừa nghe không rõ thì phải bị
        //     từ chối, không phải hỏi lại — hỏi lại một lệnh đằng nào cũng bị
        //     chặn chỉ làm tài xế mất thêm một lượt nói trong lúc đang lái.
        //
        //  2. "NGHE KHÔNG RÕ" trước "CÓ CHẮC KHÔNG". Hỏi *"bạn có chắc muốn mở
        //     khoá cửa không?"* khi máy chưa nghe rõ là đã giả định hiểu đúng
        //     lệnh — người dùng gật đầu cho một câu mà máy có thể nghe nhầm.
        //     Câu đúng trong tình huống đó là *"nhắc lại giúp mình"*.
        //
        //  3. Còn lại mới tới xác nhận hành động.
        rangeCheck(request)?.let { return it }
        doorDenyRules(request, state)?.let { return it }
        confidenceRule(request)?.let { return it }
        doorConfirmRule(request)?.let { return it }
        return Verdict.Allow
    }

    private fun rangeCheck(request: VehicleWriteRequest): Verdict? {
        if (request.propertyId != VehicleProperties.HVAC_TEMPERATURE_SET) return null
        val celsius = (request.value as? Number)?.toDouble() ?: return null
        if (celsius.isNaN() || celsius < MIN_TEMPERATURE_C || celsius > MAX_TEMPERATURE_C) {
            return Verdict.Deny(
                rule = SafetyRules.VALUE_RANGE,
                reasonVi = "Nhiệt độ hỗ trợ từ ${MIN_TEMPERATURE_C.toInt()} đến " +
                    "${MAX_TEMPERATURE_C.toInt()} độ C.",
                suggestion = "Bạn chọn một mức trong khoảng đó nhé.",
            )
        }
        return null
    }

    /**
     * G1_SPEED_LOCK và G1_GEAR_LOCK — hai luật **từ chối dứt khoát**.
     *
     * Chỉ áp cho **mở khoá** (`false`). Khoá cửa lúc đang chạy là hành động an
     * toàn, chặn nó thì vô lý.
     */
    private fun doorDenyRules(request: VehicleWriteRequest, state: VehicleSafetyState): Verdict? {
        if (!isDoorUnlock(request)) return null

        val speedKmh = state.speedKmh
        if (speedKmh == null || !speedKmh.isFinite() || speedKmh < 0f) {
            return Verdict.Deny(
                rule = SafetyRules.STALE_STATE,
                reasonVi = "Mình chưa đọc được tốc độ hiện tại nên chưa thể mở khoá cửa.",
                suggestion = "Bạn kiểm tra trạng thái xe rồi thử lại nhé.",
            )
        }

        if (speedKmh > MAX_UNLOCK_SPEED_KMH) {
            return Verdict.Deny(
                rule = SafetyRules.SPEED_LOCK,
                reasonVi = "Xe đang chạy, mình chưa mở cửa được.",
                suggestion = "Bạn dừng hẳn rồi nói lại nhé.",
            )
        }

        // G1_GEAR_LOCK chỉ chạy khi thật sự đọc được số. Không đọc được thì im
        // lặng — xem ghi chú ở đầu class.
        if (state.gear != null && state.gear != PARK_GEAR) {
            return Verdict.Deny(
                rule = SafetyRules.GEAR_LOCK,
                reasonVi = "Xe chưa về số P nên mình chưa mở cửa được.",
                suggestion = "Bạn về số P rồi nói lại nhé.",
            )
        }

        return null
    }

    /** G2_CONFIRM_DOOR — mở khoá cửa luôn phải hỏi, kể cả khi xe đã dừng hẳn. */
    private fun doorConfirmRule(request: VehicleWriteRequest): Verdict? {
        if (!isDoorUnlock(request)) return null
        if (request.source != VehicleCommandSource.VOICE || request.isConfirmed) return null
        return Verdict.Confirm(
            rule = SafetyRules.CONFIRM_DOOR,
            // Câu hỏi phải nói luôn *cách* trả lời: ngữ pháp không có intent
            // có/không (`GrammarIntentRouter.kt:54` chỉ khớp "mở cửa" /
            // "mở khóa cửa"), nên tài xế nói "có" sẽ rơi vào Unknown và huỷ
            // luôn câu hỏi. Nhắc lại lệnh mới là câu trả lời hợp lệ.
            questionVi = "Bạn có chắc muốn mở khoá cửa không? " +
                "Nói lại “mở cửa” để xác nhận.",
        )
    }

    private fun isDoorUnlock(request: VehicleWriteRequest): Boolean =
        request.propertyId == VehicleProperties.DOOR_LOCK && request.value == false

    /** G3_LOW_CONFIDENCE — chỉ áp cho lệnh đến từ giọng nói và thuộc nhóm nhạy cảm. */
    private fun confidenceRule(request: VehicleWriteRequest): Verdict? {
        val confidence = request.confidence ?: return null
        if (request.propertyId !in SENSITIVE_PROPERTIES) return null
        if (confidence >= MIN_CONFIDENCE) return null
        return Verdict.Confirm(
            rule = SafetyRules.LOW_CONFIDENCE,
            questionVi = "Mình nghe chưa rõ. Bạn nhắc lại giúp mình nhé?",
        )
    }

    private companion object {
        const val MAX_UNLOCK_SPEED_KMH = 5f
        const val MIN_CONFIDENCE = 0.6f
        const val PARK_GEAR = "P"
        const val MIN_TEMPERATURE_C = 16.0
        const val MAX_TEMPERATURE_C = 32.0

        val SENSITIVE_PROPERTIES = setOf(VehicleProperties.DOOR_LOCK)
    }
}
