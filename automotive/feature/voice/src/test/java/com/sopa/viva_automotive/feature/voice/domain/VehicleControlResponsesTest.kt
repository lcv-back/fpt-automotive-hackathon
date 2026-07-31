package com.sopa.viva_automotive.feature.voice.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleControlResponsesTest {

    @Test
    fun `temperature confirmation describes the target rather than cabin state`() {
        assertEquals(
            "Đã đặt nhiệt độ mục tiêu 24°C.",
            VehicleControlResponses.temperatureTarget(24f),
        )
        assertEquals(
            "Đã đặt nhiệt độ mục tiêu 22,5°C.",
            VehicleControlResponses.temperatureTarget(22.5f),
        )
    }

    @Test
    fun `fan confirmation uses the applied level`() {
        assertEquals("Đã đặt quạt mức 5.", VehicleControlResponses.fanSpeed(5))
    }

    @Test
    fun `door confirmation states the v1 driver door scope`() {
        assertEquals("Đã khóa cửa tài xế.", VehicleControlResponses.driverDoor(locked = true))
        assertEquals("Đã mở khóa cửa tài xế.", VehicleControlResponses.driverDoor(locked = false))
    }
}
