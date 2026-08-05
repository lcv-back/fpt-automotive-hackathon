package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockVehicleRepositoryTest {

    @Test
    fun `seeded defaults are readable`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = false)

        val temp = repository.getProperty(
            VehicleProperties.HVAC_TEMPERATURE_SET,
            VehicleAreas.SEAT_ZONE_DRIVER,
        )

        assertEquals(22f, temp.getOrThrow().floatValue())
    }

    @Test
    fun `observe emits current value on collection`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = false)

        val first = repository.observeProperty(VehicleProperties.HVAC_FAN_SPEED).first()

        assertEquals(3, first.intValue())
    }

    @Test
    fun `set property is reflected in observers`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = false)

        repository.setProperty(
            VehicleProperties.HVAC_TEMPERATURE_SET,
            VehicleAreas.SEAT_ZONE_DRIVER,
            25.5f,
        ).getOrThrow()

        val updated = repository.observeProperty(VehicleProperties.HVAC_TEMPERATURE_SET)
            .first { it.areaId == VehicleAreas.SEAT_ZONE_DRIVER && it.floatValue() == 25.5f }
        assertEquals(25.5f, updated.floatValue())
    }

    /**
     * A1 và E09 đứng hoặc sụp ở đây: kịch bản *"mở cửa lúc xe đang chạy"* chỉ
     * tái lập được nếu tốc độ đứng yên ở giá trị người kiểm thử đặt. Vòng mô
     * phỏng quét 0→90 km/h theo hình sin, nên không ghim thì cùng một thao tác
     * lúc bị `G1_SPEED_LOCK` chặn, lúc lại được cho qua.
     */
    @Test
    fun `an injected speed survives the simulator instead of being overwritten`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = true)
        // Vòng mô phỏng chỉ chạy khi có người quan sát.
        backgroundScope.launch { repository.observeProperty(VehicleProperties.PERF_VEHICLE_SPEED).collect { } }

        advanceTimeBy(3_000)
        runCurrent()
        val simulated = speedOf(repository)
        assertNotEquals("vòng mô phỏng phải thật sự chạy trong test này", 0f, simulated)

        repository.injectVehicleEvent(
            VehicleProperties.PERF_VEHICLE_SPEED,
            VehicleAreas.GLOBAL,
            60f,
        )
        advanceTimeBy(10_000)
        runCurrent()

        assertEquals(60f, speedOf(repository))
    }

    @Test
    fun `pinning one property leaves the rest of the simulation alone`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = true)
        backgroundScope.launch { repository.observeProperty(VehicleProperties.PERF_VEHICLE_SPEED).collect { } }
        repository.injectVehicleEvent(
            VehicleProperties.PERF_VEHICLE_SPEED,
            VehicleAreas.GLOBAL,
            60f,
        )

        // Nhiệt độ hiện tại vẫn phải bò về mức đặt: ghim tốc độ không được làm
        // đứng cả chiếc xe.
        repository.setProperty(
            VehicleProperties.HVAC_TEMPERATURE_SET,
            VehicleAreas.SEAT_ZONE_DRIVER,
            18f,
        ).getOrThrow()
        val before = repository
            .getProperty(VehicleProperties.HVAC_TEMPERATURE_CURRENT, VehicleAreas.SEAT_ZONE_DRIVER)
            .getOrThrow().floatValue()!!

        advanceTimeBy(5_000)
        runCurrent()

        val after = repository
            .getProperty(VehicleProperties.HVAC_TEMPERATURE_CURRENT, VehicleAreas.SEAT_ZONE_DRIVER)
            .getOrThrow().floatValue()!!
        assertTrue("nhiệt độ phải tiếp tục hội tụ: $before -> $after", after < before)
        assertEquals("tốc độ vẫn phải bị ghim", 60f, speedOf(repository))
    }

    /** Lệnh thật từ app vẫn phải ghi được lên property đã ghim. */
    @Test
    fun `pinning blocks the simulator, not real writes from the app`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = true)
        repository.injectVehicleEvent(
            VehicleProperties.DOOR_LOCK,
            VehicleAreas.DOOR_ROW_1_LEFT,
            true,
        )

        repository.setProperty(
            VehicleProperties.DOOR_LOCK,
            VehicleAreas.DOOR_ROW_1_LEFT,
            false,
        ).getOrThrow()

        assertEquals(
            false,
            repository.getProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT)
                .getOrThrow().value,
        )
    }

    private suspend fun speedOf(repository: MockVehicleRepository): Float =
        repository.getProperty(VehicleProperties.PERF_VEHICLE_SPEED, VehicleAreas.GLOBAL)
            .getOrThrow().floatValue()!!

    @Test
    fun `setting an unknown property fails`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = false)

        val result = repository.setProperty(propertyId = 12345, areaId = 0, value = 1)

        assertTrue(result.isFailure)
    }
}
