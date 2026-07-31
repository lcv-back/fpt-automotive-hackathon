package com.sopa.viva_automotive.vehicleservice.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FanSpeedTest {

    @Test
    fun `valid levels are zero through five`() {
        assertTrue(FanSpeed.isValid(0))
        assertTrue(FanSpeed.isValid(5))
        assertFalse(FanSpeed.isValid(-1))
        assertFalse(FanSpeed.isValid(6))
    }

    @Test
    fun `clamp never produces a level above five`() {
        assertEquals(0, FanSpeed.clamp(-1))
        assertEquals(5, FanSpeed.clamp(6))
    }
}
