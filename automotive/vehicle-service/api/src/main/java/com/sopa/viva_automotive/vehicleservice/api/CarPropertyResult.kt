package com.sopa.viva_automotive.vehicleservice.api

enum class PropertyStatus { AVAILABLE, UNAVAILABLE, ERROR }

data class CarPropertyResult(
    val propertyId: Int,
    val areaId: Int,
    val value: Any,
    val timestampNanos: Long = 0L,
    val status: PropertyStatus = PropertyStatus.AVAILABLE,
) {
    fun floatValue(): Float? = (value as? Number)?.toFloat()
    fun intValue(): Int? = (value as? Number)?.toInt()
    fun booleanValue(): Boolean? = value as? Boolean
}
