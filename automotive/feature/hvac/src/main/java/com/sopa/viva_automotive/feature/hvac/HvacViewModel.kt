package com.sopa.viva_automotive.feature.hvac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sopa.viva_automotive.core.common.units.TemperatureUnits
import com.sopa.viva_automotive.core.database.settings.SettingsDataStore
import com.sopa.viva_automotive.vehicleservice.api.ClimateState
import com.sopa.viva_automotive.vehicleservice.api.ClimateStateObserver
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleZone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HvacViewModel @Inject constructor(
    climateStateObserver: ClimateStateObserver,
    settingsDataStore: SettingsDataStore,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

        val climate: StateFlow<ClimateState> = climateStateObserver.climateState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClimateState())

        val useFahrenheit: StateFlow<Boolean> = settingsDataStore.settings
        .map { it.useFahrenheit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

        fun adjustTemperature(zone: VehicleZone, increase: Boolean) {
        val current = when (zone) {
            VehicleZone.PASSENGER -> climate.value.passengerTempCelsius
            else -> climate.value.driverTempCelsius
        }
        val delta = TemperatureUnits.stepDeltaCelsius(useFahrenheit.value)
            .let { if (increase) it else -it }
        val target = TemperatureUnits.snapCelsius(current + delta)
            .coerceIn(TemperatureUnits.MIN_CELSIUS, TemperatureUnits.MAX_CELSIUS)
        setProperty(VehicleProperties.HVAC_TEMPERATURE_SET, zone.areaId, target)
    }

    fun adjustFanSpeed(delta: Int) {
        val target = (climate.value.fanSpeed + delta).coerceIn(MIN_FAN, MAX_FAN)
        setProperty(VehicleProperties.HVAC_FAN_SPEED, 0, target)
    }

    fun setFanDirection(direction: Int) =
        setProperty(VehicleProperties.HVAC_FAN_DIRECTION, 0, direction)

    fun setAc(on: Boolean) = setProperty(VehicleProperties.HVAC_AC_ON, 0, on)

    fun setAuto(on: Boolean) = setProperty(VehicleProperties.HVAC_AUTO_ON, 0, on)

    fun setHvacPower(on: Boolean) = setProperty(VehicleProperties.HVAC_POWER_ON, 0, on)

    private fun setProperty(propertyId: Int, areaId: Int, value: Any) {
        viewModelScope.launch {
            vehicleRepository.setProperty(propertyId, areaId, value).onFailure { error ->
                _errors.tryEmit(error.message ?: "Climate command failed")
            }
        }
    }

    private companion object {
        const val MIN_FAN = 0
        const val MAX_FAN = 6
    }
}
