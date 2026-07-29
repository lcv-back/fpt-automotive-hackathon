package com.sopa.viva_automotive.feature.voice.domain

import com.sopa.viva_automotive.core.common.units.TemperatureUnits
import com.sopa.viva_automotive.core.database.settings.SettingsDataStore
import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleZone
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

class CommandValidationException(message: String) : IllegalArgumentException(message)

class ExecuteVehicleControlUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val settingsDataStore: SettingsDataStore,
) {

    suspend operator fun invoke(intent: VehicleIntent): Result<String> = when (intent) {
        is VehicleIntent.SetTemperature -> setTemperature(intent.temperatureCelsius, intent.zone)

        is VehicleIntent.AdjustTemperature -> {
            val zone = concreteZones(intent.zone).first()
            currentFloat(VehicleProperties.HVAC_TEMPERATURE_SET, zone.areaId).mapCatching { current ->
                val target = (current + intent.deltaCelsius).coerceIn(
                    TemperatureUnits.MIN_CELSIUS.toDouble(),
                    TemperatureUnits.MAX_CELSIUS.toDouble(),
                )
                setTemperature(target, intent.zone).getOrThrow()
            }
        }

        is VehicleIntent.SetFanSpeed -> {
            if (intent.level !in MIN_FAN..MAX_FAN) {
                Result.failure(
                    CommandValidationException("Fan speed must be between $MIN_FAN and $MAX_FAN"),
                )
            } else {
                vehicleRepository
                    .setProperty(VehicleProperties.HVAC_FAN_SPEED, VehicleAreas.GLOBAL, intent.level)
                    .map { "Fan speed set to ${intent.level}" }
            }
        }

        is VehicleIntent.AdjustFanSpeed ->
            currentInt(VehicleProperties.HVAC_FAN_SPEED, VehicleAreas.GLOBAL).mapCatching { current ->
                val target = (current + intent.delta).coerceIn(MIN_FAN, MAX_FAN)
                vehicleRepository
                    .setProperty(VehicleProperties.HVAC_FAN_SPEED, VehicleAreas.GLOBAL, target)
                    .getOrThrow()
                "Fan speed set to $target"
            }

        is VehicleIntent.SetAc ->
            vehicleRepository
                .setProperty(VehicleProperties.HVAC_AC_ON, VehicleAreas.GLOBAL, intent.on)
                .map { if (intent.on) "Air conditioning on" else "Air conditioning off" }

        is VehicleIntent.SetHvacPower ->
            vehicleRepository
                .setProperty(VehicleProperties.HVAC_POWER_ON, VehicleAreas.GLOBAL, intent.on)
                .map { if (intent.on) "Climate system on" else "Climate system off" }

        is VehicleIntent.SetDoorLock ->
            vehicleRepository
                .setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, intent.locked)
                .map { if (intent.locked) "Doors locked" else "Doors unlocked" }

        is VehicleIntent.QueryStatus -> queryStatus(intent.kind)

        is VehicleIntent.Unknown ->
            Result.failure(
                CommandValidationException("Sorry, I didn't understand \"${intent.utterance}\""),
            )
    }

    private suspend fun setTemperature(celsius: Double, zone: VehicleZone): Result<String> {
        if (celsius < TemperatureUnits.MIN_CELSIUS || celsius > TemperatureUnits.MAX_CELSIUS) {
            return Result.failure(
                CommandValidationException(
                    "Temperature must be between ${TemperatureUnits.MIN_CELSIUS.roundToInt()} " +
                        "and ${TemperatureUnits.MAX_CELSIUS.roundToInt()} degrees Celsius",
                ),
            )
        }
        val value = celsius.toFloat()
        for (target in concreteZones(zone)) {
            val result = vehicleRepository.setProperty(
                VehicleProperties.HVAC_TEMPERATURE_SET,
                target.areaId,
                value,
            )
            if (result.isFailure) return result.map { "" }
        }
        val zoneLabel = when (zone) {
            VehicleZone.DRIVER -> "driver"
            VehicleZone.PASSENGER -> "passenger"
            VehicleZone.ALL -> "all zones"
        }
        return Result.success("Temperature set to ${formatTemp(value)} for $zoneLabel")
    }

    private suspend fun queryStatus(kind: VehicleIntent.StatusQueryKind): Result<String> =
        when (kind) {
            VehicleIntent.StatusQueryKind.SPEED ->
                currentFloat(VehicleProperties.PERF_VEHICLE_SPEED, VehicleAreas.GLOBAL)
                    .map { "Current speed is ${(it * 3.6f).roundToInt()} kilometers per hour" }
            VehicleIntent.StatusQueryKind.FUEL ->
                currentFloat(VehicleProperties.FUEL_LEVEL, VehicleAreas.GLOBAL)
                    .map { "Fuel level is ${it.roundToInt()} percent" }
            VehicleIntent.StatusQueryKind.BATTERY ->
                currentFloat(VehicleProperties.EV_BATTERY_LEVEL, VehicleAreas.GLOBAL)
                    .map { "Battery level is ${it.roundToInt()} percent" }
            VehicleIntent.StatusQueryKind.TEMPERATURE ->
                currentFloat(VehicleProperties.HVAC_TEMPERATURE_SET, VehicleAreas.SEAT_ZONE_DRIVER)
                    .map { "Temperature is set to ${formatTemp(it)}" }
        }

    private suspend fun formatTemp(celsius: Float): String {
        val useFahrenheit = settingsDataStore.settings.first().useFahrenheit
        return TemperatureUnits.format(celsius, useFahrenheit)
    }

        private fun concreteZones(zone: VehicleZone): List<VehicleZone> = when (zone) {
        VehicleZone.ALL -> listOf(VehicleZone.DRIVER, VehicleZone.PASSENGER)
        else -> listOf(zone)
    }

    private suspend fun currentFloat(propertyId: Int, areaId: Int): Result<Float> =
        vehicleRepository.getProperty(propertyId, areaId).mapCatching {
            it.floatValue() ?: error("Property $propertyId has no numeric value")
        }

    private suspend fun currentInt(propertyId: Int, areaId: Int): Result<Int> =
        vehicleRepository.getProperty(propertyId, areaId).mapCatching {
            it.intValue() ?: error("Property $propertyId has no numeric value")
        }

    private companion object {
        const val MIN_FAN = 0
        const val MAX_FAN = 6
    }
}
