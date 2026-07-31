package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.FanDirection
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VstateCommandsTest {

    @Test
    fun `ac on maps to HVAC_AC_ON boolean`() {
        val write = VstateCommands.parse("ac", "1").getOrThrow()

        assertEquals(VehicleProperties.HVAC_AC_ON, write.propertyId)
        assertEquals(VehicleAreas.GLOBAL, write.areaId)
        assertEquals(true, write.value)
    }

    @Test
    fun `boolean states accept on-off words`() {
        assertEquals(false, VstateCommands.parse("hvac_power", "off").getOrThrow().value)
        assertEquals(true, VstateCommands.parse("door_lock", "true").getOrThrow().value)
    }

    @Test
    fun `driver temperature maps to zoned float`() {
        val write = VstateCommands.parse("temp_driver", "22.5").getOrThrow()

        assertEquals(VehicleProperties.HVAC_TEMPERATURE_SET, write.propertyId)
        assertEquals(VehicleAreas.SEAT_ZONE_DRIVER, write.areaId)
        assertEquals(22.5f, write.value)
    }

    @Test
    fun `fan speed level maps to int`() {
        val write = VstateCommands.parse("fan_speed", "6").getOrThrow()

        assertEquals(VehicleProperties.HVAC_FAN_SPEED, write.propertyId)
        assertEquals(6, write.value)
    }

    @Test
    fun `fan direction accepts names and numbers`() {
        assertEquals(
            FanDirection.DEFROST,
            VstateCommands.parse("fan_direction", "defrost").getOrThrow().value,
        )
        assertEquals(
            FanDirection.FACE_AND_FLOOR,
            VstateCommands.parse("fan_direction", "3").getOrThrow().value,
        )
    }

    @Test
    fun `door open event maps to door position`() {
        val write = VstateCommands.parse("door", "1").getOrThrow()

        assertEquals(VehicleProperties.DOOR_POS, write.propertyId)
        assertEquals(VehicleAreas.DOOR_ROW_1_LEFT, write.areaId)
        assertEquals(1, write.value)
    }

    @Test
    fun `unknown unit type fails`() {
        assertTrue(VstateCommands.parse("warp_drive", "1").isFailure)
    }

    @Test
    fun `missing state value fails`() {
        assertTrue(VstateCommands.parse("ac", null).isFailure)
    }

    @Test
    fun `malformed number fails instead of crashing`() {
        assertTrue(VstateCommands.parse("fan_speed", "fast").isFailure)
    }
}
