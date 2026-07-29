package com.sopa.viva_automotive.vehicleservice.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

data class ClimateState(
    val driverTempCelsius: Float = 22f,
    val passengerTempCelsius: Float = 22f,
    val fanSpeed: Int = 0,
    val fanDirection: Int = FanDirection.FACE,
    val acOn: Boolean = false,
    val autoOn: Boolean = false,
    val hvacPowerOn: Boolean = true,
) {
    internal fun update(prop: CarPropertyResult): ClimateState = when (prop.propertyId) {
        VehicleProperties.HVAC_TEMPERATURE_SET -> when {
            prop.areaId and VehicleAreas.SEAT_ZONE_PASSENGER != 0 ->
                copy(passengerTempCelsius = prop.floatValue() ?: passengerTempCelsius)
            else -> copy(driverTempCelsius = prop.floatValue() ?: driverTempCelsius)
        }
        VehicleProperties.HVAC_FAN_SPEED -> copy(fanSpeed = prop.intValue() ?: fanSpeed)
        VehicleProperties.HVAC_FAN_DIRECTION -> copy(fanDirection = prop.intValue() ?: fanDirection)
        VehicleProperties.HVAC_AC_ON -> copy(acOn = prop.booleanValue() ?: acOn)
        VehicleProperties.HVAC_AUTO_ON -> copy(autoOn = prop.booleanValue() ?: autoOn)
        VehicleProperties.HVAC_POWER_ON -> copy(hvacPowerOn = prop.booleanValue() ?: hvacPowerOn)
        else -> this
    }
}

@Singleton
class ClimateStateObserver @Inject constructor(
    private val vehicle: VehicleRepository,
) {
    val climateState: Flow<ClimateState> = merge(
        vehicle.observeProperty(VehicleProperties.HVAC_TEMPERATURE_SET),
        vehicle.observeProperty(VehicleProperties.HVAC_FAN_SPEED),
        vehicle.observeProperty(VehicleProperties.HVAC_FAN_DIRECTION),
        vehicle.observeProperty(VehicleProperties.HVAC_AC_ON),
        vehicle.observeProperty(VehicleProperties.HVAC_AUTO_ON),
        vehicle.observeProperty(VehicleProperties.HVAC_POWER_ON),
    ).scan(ClimateState()) { state, prop -> state.update(prop) }
}
