package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    @Test
    fun `setting an unknown property fails`() = runTest {
        val repository = MockVehicleRepository(backgroundScope, simulate = false)

        val result = repository.setProperty(propertyId = 12345, areaId = 0, value = 1)

        assertTrue(result.isFailure)
    }
}
