package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import org.junit.Assert.assertEquals
import org.junit.Test

class AreaIdResolverTest {

    @Test
    fun `exact match is returned unchanged`() {
        val declared = intArrayOf(0x01, 0x04)

        assertEquals(listOf(0x01), AreaIdResolver.resolve(declared, 0x01))
        assertEquals(listOf(0x04), AreaIdResolver.resolve(declared, 0x04))
    }

    @Test
    fun `passenger mask 0x44 resolves to ROW_1_RIGHT when that is declared`() {
        val declared = intArrayOf(
            VehicleAreas.DOOR_ROW_1_LEFT, // 0x1 — also the driver temperature zone
            0x04, // ROW_1_RIGHT
        )

        assertEquals(
            listOf(0x04),
            AreaIdResolver.resolve(declared, VehicleAreas.SEAT_ZONE_PASSENGER),
        )
    }

    @Test
    fun `driver mask resolves to overlapping left-side areas`() {
        val declared = intArrayOf(0x01, 0x04)

        assertEquals(
            listOf(0x01),
            AreaIdResolver.resolve(declared, VehicleAreas.SEAT_ZONE_DRIVER),
        )
    }

    @Test
    fun `GLOBAL request fans out to every declared area`() {
        val declared = intArrayOf(0x01, 0x04)

        assertEquals(listOf(0x01, 0x04), AreaIdResolver.resolve(declared, VehicleAreas.GLOBAL))
    }

    @Test
    fun `unknown config returns the requested id unchanged`() {
        assertEquals(listOf(0x44), AreaIdResolver.resolve(null, 0x44))
        assertEquals(listOf(0x44), AreaIdResolver.resolve(intArrayOf(), 0x44))
    }

    @Test
    fun `no-overlap request falls back to the requested id`() {
        assertEquals(listOf(0x20), AreaIdResolver.resolve(intArrayOf(0x01, 0x04), 0x20))
    }
}
