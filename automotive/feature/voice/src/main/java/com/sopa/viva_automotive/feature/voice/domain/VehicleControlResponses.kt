package com.sopa.viva_automotive.feature.voice.domain

import java.math.BigDecimal

object VehicleControlResponses {

    fun temperatureTarget(celsius: Float): String =
        "Đã đặt nhiệt độ mục tiêu ${formatNumber(celsius)}°C."

    fun fanSpeed(level: Int): String = "Đã đặt quạt mức $level."

    fun driverDoor(locked: Boolean): String =
        if (locked) "Đã khóa cửa tài xế." else "Đã mở khóa cửa tài xế."

    private fun formatNumber(value: Float): String =
        BigDecimal.valueOf(value.toDouble()).stripTrailingZeros().toPlainString().replace('.', ',')
}
