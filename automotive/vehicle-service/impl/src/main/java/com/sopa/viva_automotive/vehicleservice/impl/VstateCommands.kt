package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.FanDirection
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties

object VstateCommands {

    data class PropertyWrite(val propertyId: Int, val areaId: Int, val value: Any)

        fun parse(unitType: String?, stateValue: String?): Result<PropertyWrite> = runCatching {
        requireNotNull(unitType) { "Missing unit_type" }
        requireNotNull(stateValue) { "Missing state_value" }

        when (unitType.lowercase()) {
            "hvac_power" -> global(VehicleProperties.HVAC_POWER_ON, stateValue.toVehicleBoolean())
            "ac" -> global(VehicleProperties.HVAC_AC_ON, stateValue.toVehicleBoolean())
            "hvac_auto" -> global(VehicleProperties.HVAC_AUTO_ON, stateValue.toVehicleBoolean())
            "fan_speed" -> global(VehicleProperties.HVAC_FAN_SPEED, stateValue.toInt())
            "fan_direction" -> global(VehicleProperties.HVAC_FAN_DIRECTION, stateValue.toFanDirection())
            "temp_driver" -> PropertyWrite(
                VehicleProperties.HVAC_TEMPERATURE_SET,
                VehicleAreas.SEAT_ZONE_DRIVER,
                stateValue.toFloat(),
            )
            "temp_passenger" -> PropertyWrite(
                VehicleProperties.HVAC_TEMPERATURE_SET,
                VehicleAreas.SEAT_ZONE_PASSENGER,
                stateValue.toFloat(),
            )
            "temp_cabin" -> PropertyWrite(
                VehicleProperties.HVAC_TEMPERATURE_CURRENT,
                VehicleAreas.SEAT_ZONE_DRIVER,
                stateValue.toFloat(),
            )

            "door_lock" -> PropertyWrite(
                VehicleProperties.DOOR_LOCK,
                VehicleAreas.DOOR_ROW_1_LEFT,
                stateValue.toVehicleBoolean(),
            )
            "door" -> PropertyWrite(
                VehicleProperties.DOOR_POS,
                VehicleAreas.DOOR_ROW_1_LEFT,
                stateValue.toInt(),
            )
            "cabin_light" -> global(VehicleProperties.CABIN_LIGHTS_SWITCH, stateValue.toInt())

            "speed" -> global(VehicleProperties.PERF_VEHICLE_SPEED, stateValue.toFloat())
            "fuel" -> global(VehicleProperties.FUEL_LEVEL, stateValue.toFloat())
            "battery" -> global(VehicleProperties.EV_BATTERY_LEVEL, stateValue.toFloat())
            "ignition" -> global(VehicleProperties.IGNITION_STATE, stateValue.toInt())

            else -> throw IllegalArgumentException("Unknown unit_type: $unitType")
        }
    }

    private fun global(propertyId: Int, value: Any) =
        PropertyWrite(propertyId, VehicleAreas.GLOBAL, value)

    private fun String.toVehicleBoolean(): Boolean = when (lowercase()) {
        "1", "true", "on" -> true
        "0", "false", "off" -> false
        else -> throw IllegalArgumentException("Expected boolean state, got: $this")
    }

    private fun String.toFanDirection(): Int = when (lowercase()) {
        "face" -> FanDirection.FACE
        "floor" -> FanDirection.FLOOR
        "face_floor" -> FanDirection.FACE_AND_FLOOR
        "defrost" -> FanDirection.DEFROST
        else -> toInt()
    }
}
