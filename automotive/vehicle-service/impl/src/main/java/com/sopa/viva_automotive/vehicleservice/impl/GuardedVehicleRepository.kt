package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.CarPropertyResult
import com.sopa.viva_automotive.vehicleservice.api.PropertyStatus
import com.sopa.viva_automotive.vehicleservice.api.SafetyGuard
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleSafetyState
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteRequest
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteContext
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
) : VehicleRepository {

    override fun observeProperty(propertyId: Int): Flow<CarPropertyResult> =
        delegate.observeProperty(propertyId)

    override suspend fun getProperty(propertyId: Int, areaId: Int): Result<CarPropertyResult> =
        delegate.getProperty(propertyId, areaId)

    override suspend fun setProperty(
        propertyId: Int,
        areaId: Int,
        value: Any,
        context: VehicleWriteContext,
    ): Result<Unit> {
        val request = VehicleWriteRequest(
            propertyId = propertyId,
            areaId = areaId,
            value = value,
            confidence = context.confidence,
            source = context.source,
            isConfirmed = context.isConfirmed,
        )
        return when (val verdict = guard.evaluate(request, safetyStateFor(request))) {
            is Verdict.Allow -> delegate.setProperty(propertyId, areaId, value, context)

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

    /**
     * Door unlock is the only v1 rule that needs live vehicle state. Read it
     * from the unguarded delegate in the same suspend operation as the write;
     * callers cannot accidentally inject a stale or synthetic speed snapshot.
     *
     * AAOS exposes `PERF_VEHICLE_SPEED` in m/s while the policy contract uses
     * km/h, so this is the single conversion boundary. Any read/status/type
     * failure becomes `speedKmh = null`, which `DefaultSafetyGuard` denies via
     * `G1_STALE_STATE` before the delegate setter can run.
     */
    private suspend fun safetyStateFor(request: VehicleWriteRequest): VehicleSafetyState {
        if (!request.isDoorUnlock()) return VehicleSafetyState()

        val speed = delegate
            .getProperty(VehicleProperties.PERF_VEHICLE_SPEED, VehicleAreas.GLOBAL)
            .getOrNull()
            ?.takeIf { it.status == PropertyStatus.AVAILABLE }

        return VehicleSafetyState(
            speedKmh = speed?.floatValue()?.times(METERS_PER_SECOND_TO_KMH),
            timestampNanos = speed?.timestampNanos ?: 0L,
        )
    }

    private fun VehicleWriteRequest.isDoorUnlock(): Boolean =
        propertyId == VehicleProperties.DOOR_LOCK && value == false

    companion object {
        private const val METERS_PER_SECOND_TO_KMH = 3.6f

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
