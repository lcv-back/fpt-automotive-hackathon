package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.SafetyGuard
import com.sopa.viva_automotive.vehicleservice.api.SafetyRules
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleSafetyState
import com.sopa.viva_automotive.vehicleservice.api.Verdict
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
        speedKmh: Float,
        guard: SafetyGuard = DefaultSafetyGuard(),
    ) = GuardedVehicleRepository(
        delegate = underlying,
        guard = guard,
        stateProvider = { VehicleSafetyState(speedKmh = speedKmh) },
    )

    private suspend fun VehicleRepository.readDoorLock(): Boolean? =
        getProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT)
            .getOrNull()?.booleanValue()

    @Test
    fun `lenh bi Deny thi KHONG ghi xuong xe`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying, speedKmh = 60f)
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
        val repo = guarded(underlying, speedKmh = 0f)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        val result = repo.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, false)

        assertTrue("${result.exceptionOrNull()}", result.exceptionOrNull() is SafetyConfirmationRequiredException)
        assertEquals(true, underlying.readDoorLock())
    }

    @Test
    fun `lenh duoc Allow thi di xuong binh thuong`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying, speedKmh = 0f)

        val result = repo.setProperty(VehicleProperties.HVAC_TEMPERATURE_SET, VehicleAreas.SEAT_ZONE_DRIVER, 22f)

        assertTrue("${result.exceptionOrNull()}", result.isSuccess)
    }

    @Test
    fun `doc thi khong bi chan du dang chay`() = runTest {
        val underlying = MockVehicleRepository(backgroundScope, simulate = false)
        val repo = guarded(underlying, speedKmh = 120f)
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
        val repo = guarded(underlying, speedKmh = 60f, guard = noGuard)
        underlying.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)

        val result = repo.setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, false)

        assertTrue(result.isSuccess)
        assertEquals(false, underlying.readDoorLock())
    }
}
