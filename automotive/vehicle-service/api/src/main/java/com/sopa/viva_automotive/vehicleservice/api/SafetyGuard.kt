package com.sopa.viva_automotive.vehicleservice.api

/**
 * Tầng an toàn giữa "lệnh đã hiểu" và "ghi xuống xe" — `03-contracts.md` §4.
 *
 * Đặt ở **module `vehicle-service:api`** chứ không ở đường voice, vì có **ba**
 * nơi ghi property: `ExecuteVehicleControlUseCase` (voice), `HvacViewModel` và
 * `VehicleStatusViewModel` (HMI). Guard nào chỉ chắn đường voice thì hai màn
 * hình kia vẫn ghi thẳng, và câu "không lệnh nào tới xe mà không qua tầng an
 * toàn" trở thành lời khai sai — thứ chỉ cần mở code ra là thấy.
 *
 * Vì vậy guard được áp ở **biên `VehicleRepository`** (xem
 * `GuardedVehicleRepository` trong module impl): mọi caller đều đi qua, không
 * có đường vòng.
 *
 * ⚠️ Đây **chưa phải** tầng *service fw* mà mentor vẽ ở `03-contracts.md` §0.1.
 * Nó là chốt chặn một tầng thấp hơn, trong app. Khi `VivaCarService` (M1) có
 * mặt thì chuyển nguyên khối này vào trong đó — là việc dời file, không phải
 * viết lại. Cho tới lúc đó, **khai đúng như vậy** trong write-up.
 */
fun interface SafetyGuard {
    fun evaluate(request: VehicleWriteRequest, state: VehicleSafetyState): Verdict
}

/** Một yêu cầu ghi property, đã đủ thông tin để phán quyết. */
data class VehicleWriteRequest(
    val propertyId: Int,
    val areaId: Int,
    val value: Any,
    /**
     * Độ tin cậy của bước nhận dạng, nếu lệnh đến từ giọng nói. `null` nghĩa là
     * upstream chưa cung cấp confidence; nguồn lệnh được phân biệt riêng bằng
     * [source], không suy đoán từ trường này.
     */
    val confidence: Float? = null,
    val source: VehicleCommandSource = VehicleCommandSource.HMI,
    val isConfirmed: Boolean = false,
)

/** Nguồn của lệnh ghi, dùng để áp policy xác nhận mà không làm hỏng HMI. */
enum class VehicleCommandSource { HMI, VOICE, SYSTEM }

/**
 * Metadata bổ sung theo từng lần ghi. Giá trị mặc định giữ tương thích cho các
 * caller HMI hiện có; đường voice phải khai báo `VOICE` một cách tường minh.
 */
data class VehicleWriteContext(
    val source: VehicleCommandSource = VehicleCommandSource.HMI,
    val confidence: Float? = null,
    val isConfirmed: Boolean = false,
)

/**
 * Ảnh chụp trạng thái xe tại thời điểm phán quyết.
 *
 * Khác `VehicleState` trong `03-contracts.md` §4 ở hai chỗ, và khác có chủ đích:
 * `gear`, `parkingBrake` và `ignition` **chưa đọc được** từ `VehicleStatus` hiện
 * có (chỉ có tốc độ, mức nhiên liệu/pin, trạng thái cửa). Khai `null` và để luật
 * tương ứng **không kích hoạt** — thà một luật im lặng vì thiếu dữ liệu, còn hơn
 * một luật chạy trên giá trị mặc định bịa ra.
 */
data class VehicleSafetyState(
    /** `null` means the current speed could not be read and must not be guessed. */
    val speedKmh: Float? = null,
    val gear: String? = null,
    val parkingBrake: Boolean? = null,
    val ignition: String? = null,
    val doorsLocked: Boolean? = null,
    /** `SystemClock.elapsedRealtimeNanos()` lúc chụp; dùng để phát hiện snapshot cũ. */
    val timestampNanos: Long = 0L,
)

/** Phán quyết, ánh xạ 1-1 với `sealed class Verdict` ở §4. */
sealed interface Verdict {
    data object Allow : Verdict

    data class Deny(
        val rule: String,
        val reasonVi: String,
        val suggestion: String? = null,
    ) : Verdict

    data class Confirm(
        val rule: String,
        val questionVi: String,
    ) : Verdict
}

/** Mã luật của §4 — dùng làm khoá join cho bảng ablation N4b, đừng đổi tên. */
object SafetyRules {
    const val SPEED_LOCK = "G1_SPEED_LOCK"
    const val GEAR_LOCK = "G1_GEAR_LOCK"
    const val STALE_STATE = "G1_STALE_STATE"
    const val CONFIRM_DOOR = "G2_CONFIRM_DOOR"
    const val LOW_CONFIDENCE = "G3_LOW_CONFIDENCE"
    const val VALUE_RANGE = "G3_VALUE_RANGE"

    /**
     * `03-contracts.md` §4: *"wake phrase/trợ lý khác hoặc câu nằm ngoài 10
     * intent lõi → Deny — nói rõ phạm vi, không gọi Skill"*.
     *
     * Luật này **không** áp ở biên `VehicleRepository` như G1/G2: câu ngoài
     * phạm vi không sinh ra lệnh ghi property nào để mà chặn. Nó được áp ở chỗ
     * intent được điều phối, trước khi bất kỳ Skill nào chạy.
     */
    const val UNSUPPORTED = "G3_UNSUPPORTED"
}
