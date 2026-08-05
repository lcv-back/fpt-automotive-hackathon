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

    /**
     * Trợ lý tiếng Việt phải trả lời bằng tiếng Việt ở **mọi** intent. Trước
     * 05/08, AC/máy lạnh và toàn bộ câu trạng thái là chuỗi tiếng Anh cứng —
     * người lái nghe *"Current speed is 60 kilometers per hour"*, và vì
     * `PrerenderedPrompts` tra cứu khớp chính xác nên câu đó không trúng clip
     * nào, trên image thiếu giọng vi-VN thì tụt xuống một tiếng ping.
     */
    @Test
    fun `every spoken answer is Vietnamese`() {
        assertEquals("Đã bật điều hòa.", VehicleControlResponses.airConditioning(on = true))
        assertEquals("Đã tắt điều hòa.", VehicleControlResponses.airConditioning(on = false))
        assertEquals("Đã bật hệ thống khí hậu.", VehicleControlResponses.climatePower(on = true))
        assertEquals("Xăng còn 67 phần trăm.", VehicleControlResponses.fuelLevel(67.4f))
        assertEquals("Pin còn 80 phần trăm.", VehicleControlResponses.batteryLevel(80f))
        assertEquals("Nhiệt độ đang đặt ở 22°C.", VehicleControlResponses.temperatureSetting("22°C"))
    }

    @Test
    fun `speed is spoken in km per hour, converted from the raw m per s property`() {
        // PERF_VEHICLE_SPEED tính bằng m/s; 16.7 m/s là 60 km/h — đúng con số
        // kịch bản M7-03 dùng.
        assertEquals("Xe đang chạy 60 ki lô mét một giờ.", VehicleControlResponses.currentSpeed(16.7f))
        assertEquals("Xe đang chạy 0 ki lô mét một giờ.", VehicleControlResponses.currentSpeed(0f))
    }

    /**
     * Hai câu hỏi lại này phải **khớp từng chữ** với clip trong
     * `PrerenderedPrompts` (`tts_clarify_temperature_range`,
     * `tts_clarify_fan_level`), nếu không thì mất tiếng nói thật và chỉ còn ping.
     */
    @Test
    fun `out-of-range prompts match the pre-rendered clips word for word`() {
        assertEquals(
            "Nhiệt độ hỗ trợ từ 16 đến 32 độ C. Bạn muốn đặt bao nhiêu độ?",
            VehicleControlResponses.temperatureOutOfRange(16, 32),
        )
        assertEquals(
            "Bạn muốn đặt quạt ở mức mấy, từ 0 đến 5?",
            VehicleControlResponses.fanSpeedOutOfRange(0, 5),
        )
    }

    @Test
    fun `door confirmation states the v1 driver door scope`() {
        assertEquals("Đã khóa cửa tài xế.", VehicleControlResponses.driverDoor(locked = true))
        assertEquals("Đã mở khóa cửa tài xế.", VehicleControlResponses.driverDoor(locked = false))
    }
}
