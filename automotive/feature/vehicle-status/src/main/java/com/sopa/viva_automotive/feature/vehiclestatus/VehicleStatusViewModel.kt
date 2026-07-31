package com.sopa.viva_automotive.feature.vehiclestatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sopa.viva_automotive.vehicleservice.api.LightSwitch
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleStatus
import com.sopa.viva_automotive.vehicleservice.api.VehicleStatusObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class VehicleStatusViewModel @Inject constructor(
    vehicleStatusObserver: VehicleStatusObserver,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    val status: StateFlow<VehicleStatus> = vehicleStatusObserver.vehicleStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleStatus())

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    fun setDoorsLocked(locked: Boolean) {
        viewModelScope.launch {
            vehicleRepository
                .setProperty(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, locked)
                .onFailure { error ->
                    _errors.tryEmit(error.message ?: "Door lock command failed")
                }
        }
    }

    fun setCabinLights(on: Boolean) {
        viewModelScope.launch {
            vehicleRepository
                .setProperty(
                    VehicleProperties.CABIN_LIGHTS_SWITCH,
                    VehicleAreas.GLOBAL,
                    if (on) LightSwitch.ON else LightSwitch.OFF,
                )
                .onFailure { error ->
                    _errors.tryEmit(error.message ?: "Cabin light command failed")
                }
        }
    }
}
