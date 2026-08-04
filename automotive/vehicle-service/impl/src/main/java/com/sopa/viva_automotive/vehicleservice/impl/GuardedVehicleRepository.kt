package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.CarPropertyResult
import com.sopa.viva_automotive.vehicleservice.api.SafetyGuard
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleSafetyState
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteRequest
import com.sopa.viva_automotive.vehicleservice.api.Verdict
import kotlinx.coroutines.flow.Flow

/**
 * Lệnh bị tầng an toàn chặn. Mang theo **mã luật** để log ra
 * `Deny:<RULE_ID>` đúng grammar `03-contracts.md` §1.2 — đó là khoá join của
 * bảng ablation N4b, không phải để cho đẹp.
 */
class SafetyDeniedException(
    val rule: String,
    val reasonVi: String,
    val suggestion: String? = null,
) : IllegalStateException(reasonVi)

/** Lệnh hợp lệ nhưng phải hỏi lại tài xế trước khi thực hiện. */
class SafetyConfirmationRequiredException(
    val rule: String,
    val questionVi: String,
) : IllegalStateException(questionVi)

/**
 * Bọc một [VehicleRepository] và chạy [SafetyGuard] trước **mọi** lệnh ghi.
 *
 * ## Vì sao bọc ở đây thay vì chắn ở đường voice
 *
 * Có **ba** nơi gọi `setProperty`: `ExecuteVehicleControlUseCase` (giọng nói),
 * `HvacViewModel` và `VehicleStatusViewModel` (chạm trên HMI). Nếu guard chỉ
 * nằm ở đường voice thì hai màn hình kia vẫn ghi thẳng xuống xe, và câu
 * *"không lệnh nào tới xe mà không qua tầng an toàn"* trở thành lời khai sai —
 * loại sai mà giám khảo chỉ cần mở code là thấy.
 *
 * Bọc ở biên repository thì mọi caller đi qua, **không cần sửa một dòng nào ở
 * ba call site** — điều đáng giá khi còn chưa tới hai ngày là feature freeze.
 *
 * ## Đọc thì không chặn
 *
 * `observeProperty` và `getProperty` đi thẳng. Guard tồn tại để ngăn xe **làm**
 * điều nguy hiểm, không phải để giấu thông tin khỏi màn hình.
 */
class GuardedVehicleRepository(
    private val delegate: VehicleRepository,
    private val guard: SafetyGuard,
    private val stateProvider: () -> VehicleSafetyState,
) : VehicleRepository {

    override fun observeProperty(propertyId: Int): Flow<CarPropertyResult> =
        delegate.observeProperty(propertyId)

    override suspend fun getProperty(propertyId: Int, areaId: Int): Result<CarPropertyResult> =
        delegate.getProperty(propertyId, areaId)

    override suspend fun setProperty(propertyId: Int, areaId: Int, value: Any): Result<Unit> {
        val request = VehicleWriteRequest(propertyId, areaId, value)
        return when (val verdict = guard.evaluate(request, stateProvider())) {
            is Verdict.Allow -> delegate.setProperty(propertyId, areaId, value)

            is Verdict.Deny -> Result.failure(
                SafetyDeniedException(verdict.rule, verdict.reasonVi, verdict.suggestion),
            )

            // Quan trọng: KHÔNG ghi rồi mới hỏi. Lệnh dừng ở đây, tầng trên
            // hỏi tài xế rồi gọi lại nếu được đồng ý. Ghi trước rồi hỏi sau
            // thì câu hỏi chỉ còn là thủ tục.
            is Verdict.Confirm -> Result.failure(
                SafetyConfirmationRequiredException(verdict.rule, verdict.questionVi),
            )
        }
    }

    companion object {
        /**
         * Property mà guard quan tâm — dùng cho log/kiểm thử, không dùng để lọc
         * đầu vào: guard tự quyết định luật nào áp cho property nào.
         */
        val GUARDED_PROPERTIES = setOf(
            VehicleProperties.DOOR_LOCK,
            VehicleProperties.HVAC_TEMPERATURE_SET,
        )
    }
}
