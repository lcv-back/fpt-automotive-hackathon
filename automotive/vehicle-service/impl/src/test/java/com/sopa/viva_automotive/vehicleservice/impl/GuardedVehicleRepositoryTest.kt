package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.SafetyConfirmationRequiredException
import com.sopa.viva_automotive.vehicleservice.api.SafetyDeniedException
import com.sopa.viva_automotive.vehicleservice.api.SafetyGuard
import com.sopa.viva_automotive.vehicleservice.api.SafetyRules
import com.sopa.viva_automotive.vehicleservice.api.CarPropertyResult
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleCommandSource
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteContext
import com.sopa.viva_automotive.vehicleservice.api.Verdict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Điểm mấu chốt của bộ test này: chứng minh **lệnh bị chặn thì không chạm tới
 * xe**. Một guard trả `Deny` nhưng vẫn ghi property xuống dưới là guard vô
 * dụng, và cái vô dụng đó sẽ không lộ ra nếu chỉ test riêng bộ luật.
 */
class GuardedVehicleRepositoryTest {

    private fun guarded(
        underlying: VehicleRepository,
        guard: SafetyGuard = DefaultSafetyGuard(),
    ) = GuardedVehicleRepository(
        delegate = underlying,
        guard = guard,
    )

    private suspend fun MockVehicleRepository.setSpeedKmh(speedKmh: Float) {
        setProperty(
            VehicleProperties.PERF_VEHICLE_SPEED,
            VehicleAreas.GLOBAL,
            speedKmh / 3.6f,
        ).getOrThrow()
    }

    private suspend fun VehicleRepository.readDoorLock(): Boolean? =
        getProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT)
            .getOrNull()?.booleanValue()

    @Test
    fun `lenh bi Deny thi KHONG ghi xuong xe`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying)
        underlying.setSpeedKmh(60f)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        val result = repo.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, false)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("$error", error is SafetyDeniedException)
        assertEquals(SafetyRules.SPEED_LOCK, (error as SafetyDeniedException).rule)
        // Day moi la khang dinh quan trong: cua VAN KHOA.
        assertEquals(true, underlying.readDoorLock())
    }

    @Test
    fun `lenh can Confirm cung KHONG ghi truoc roi hoi sau`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying)
        underlying.setSpeedKmh(0f)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        val result = repo.setProperty(
            VehicleProperties.DOOR_LOCK,
            VehicleAreas.DOOR_ROW_1_LEFT,
            false,
            VehicleWriteContext(source = VehicleCommandSource.VOICE),
        )

        assertTrue("${result.exceptionOrNull()}", result.exceptionOrNull() is SafetyConfirmationRequiredException)
        assertEquals(true, underlying.readDoorLock())
    }

    @Test
    fun `lenh duoc Allow thi di xuong binh thuong`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying)

        val result = repo.setProperty(VehicleProperties.HVAC_TEMPERATURE_SET, VehicleAreas.SEAT_ZONE_DRIVER, 22f)

        assertTrue("${result.exceptionOrNull()}", result.isSuccess)
    }

    @Test
    fun `doc thi khong bi chan du dang chay`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying)
        underlying.setSpeedKmh(120f)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        // Guard ton tai de ngan xe LAM dieu nguy hiem, khong phai de giau thong
        // tin khoi man hinh.
        val read = repo.getProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT)
        assertTrue(read.isSuccess)
    }

    @Test
    fun `tat guard thi lenh nguy hiem di thang xuong xe — day la ablation A1`() = runTest {
        // Guard "no-op" chinh la bien the no-guard cua N4b. Test nay khang dinh
        // hai dieu cung luc: (1) tat duoc that, va (2) khi tat thi hau qua dung
        // nhu bang ablation mo ta — "mo cua" luc 60 km/h duoc thuc thi.
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val noGuard = SafetyGuard { _, _ -> Verdict.Allow }
        val repo = guarded(underlying, guard = noGuard)
        underlying.setSpeedKmh(60f)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        val result = repo.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, false)

        assertTrue(result.isSuccess)
        assertEquals(false, underlying.readDoorLock())
    }

    @Test
    fun `khong doc duoc toc do thi fail closed va KHONG ghi xuong xe`() = runTest {
        val real = MockVehicleRepository(backgroundScope, simulate = false)
        real.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)
        val speedUnavailable = object : VehicleRepository {
            override fun observeProperty(propertyId: Int): Flow<CarPropertyResult> =
                real.observeProperty(propertyId)

            override suspend fun getProperty(propertyId: Int, areaId: Int): Result<CarPropertyResult> =
                if (propertyId == VehicleProperties.PERF_VEHICLE_SPEED) {
                    Result.failure(IllegalStateException("speed unavailable"))
                } else {
                    real.getProperty(propertyId, areaId)
                }

            override suspend fun setProperty(
                propertyId: Int,
                areaId: Int,
                value: Any,
                context: VehicleWriteContext,
            ): Result<Unit> = real.setProperty(propertyId, areaId, value, context)
        }
        val repo = guarded(speedUnavailable)

        val result = repo.setProperty(
            VehicleProperties.DOOR_LOCK,
            VehicleAreas.DOOR_ROW_1_LEFT,
            false,
            VehicleWriteContext(source = VehicleCommandSource.VOICE),
        )

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("$error", error is SafetyDeniedException)
        assertEquals(SafetyRules.STALE_STATE, (error as SafetyDeniedException).rule)
        assertEquals(true, real.readDoorLock())
    }

    @Test
    fun `toc do NaN thi fail closed va KHONG ghi xuong xe`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying)
        underlying.setSpeedKmh(Float.NaN)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        val result = repo.setProperty(
            VehicleProperties.DOOR_LOCK,
            VehicleAreas.DOOR_ROW_1_LEFT,
            false,
            VehicleWriteContext(source = VehicleCommandSource.VOICE),
        )

        assertEquals(SafetyRules.STALE_STATE, (result.exceptionOrNull() as SafetyDeniedException).rule)
        assertEquals(true, underlying.readDoorLock())
    }

    @Test
    fun `HMI unlock khi dung yen khong bi ket trong vong Confirm`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying)
        underlying.setSpeedKmh(0f)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        val result = repo.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, false)

        assertTrue("${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(false, underlying.readDoorLock())
    }
}
