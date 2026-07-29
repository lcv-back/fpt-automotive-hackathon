package com.sopa.viva_automotive.vehicleservice.api

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

data class VehicleStatus(
    val speedMetersPerSecond: Float = 0f,
    val fuelLevelPercent: Float = 0f,
    val batteryLevelPercent: Float = 0f,
    val doorsLocked: Boolean = true,
    val driverDoorOpen: Boolean = false,
    val cabinLightsOn: Boolean = false,
) {
    val speedKmh: Float get() = speedMetersPerSecond * 3.6f

    internal fun update(prop: CarPropertyResult): VehicleStatus = when (prop.propertyId) {
        VehicleProperties.PERF_VEHICLE_SPEED ->
            copy(speedMetersPerSecond = prop.floatValue() ?: speedMetersPerSecond)
        VehicleProperties.FUEL_LEVEL ->
            copy(fuelLevelPercent = prop.floatValue() ?: fuelLevelPercent)
        VehicleProperties.EV_BATTERY_LEVEL ->
            copy(batteryLevelPercent = prop.floatValue() ?: batteryLevelPercent)
        VehicleProperties.DOOR_LOCK ->
            copy(doorsLocked = prop.booleanValue() ?: doorsLocked)
        VehicleProperties.DOOR_POS ->
            copy(driverDoorOpen = (prop.intValue() ?: 0) > 0)
        VehicleProperties.CABIN_LIGHTS_SWITCH ->
            copy(cabinLightsOn = prop.intValue() == LightSwitch.ON)
        else -> this
    }
}

@Singleton
class VehicleStatusObserver @Inject constructor(
    private val vehicle: VehicleRepository,
) {
    val vehicleStatus: Flow<VehicleStatus> = merge(
        vehicle.observeProperty(VehicleProperties.PERF_VEHICLE_SPEED),
        vehicle.observeProperty(VehicleProperties.FUEL_LEVEL),
        vehicle.observeProperty(VehicleProperties.EV_BATTERY_LEVEL),
        vehicle.observeProperty(VehicleProperties.DOOR_LOCK),
        vehicle.observeProperty(VehicleProperties.DOOR_POS),
        vehicle.observeProperty(VehicleProperties.CABIN_LIGHTS_SWITCH),
    ).scan(VehicleStatus()) { state, prop -> state.update(prop) }
}
