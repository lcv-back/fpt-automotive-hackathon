package com.sopa.viva_automotive.feature.voice.integration

import com.sopa.viva_automotive.core.common.units.TemperatureUnits
import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import com.sopa.viva_automotive.vehicleservice.api.FanSpeed
import com.viva.voice.intent.Intent

sealed interface AutomotiveVoiceAction {
    data class VehicleControl(val intent: VehicleIntent) : AutomotiveVoiceAction
    data class VolumeAdjust(val delta: Int) : AutomotiveVoiceAction
    data object MediaNext : AutomotiveVoiceAction
}

object CoreIntentMapper {
    fun map(intent: Intent): AutomotiveVoiceAction? = when (intent.name) {
        "hvac_set_temp" -> intent.number("value")
            ?.toDouble()
            ?.takeIf { value ->
                value.isFinite() && value in supportedTemperatureRange
            }
            ?.let { value ->
                AutomotiveVoiceAction.VehicleControl(
                    VehicleIntent.SetTemperature(value),
                )
            }

        "hvac_set_fan" -> intent.number("level")
            ?.toDouble()
            ?.takeIf { level ->
                level % 1.0 == 0.0 && FanSpeed.isValid(level.toInt())
            }
            ?.let { level ->
                AutomotiveVoiceAction.VehicleControl(
                    VehicleIntent.SetFanSpeed(level.toInt()),
                )
            }

        "door_lock" -> (intent.slots["lock"] as? Boolean)?.let { locked ->
            AutomotiveVoiceAction.VehicleControl(VehicleIntent.SetDoorLock(locked))
        }

        "volume_adjust" -> intent.number("delta")?.let { delta ->
            AutomotiveVoiceAction.VolumeAdjust(delta.toInt())
        }

        "media_next" -> AutomotiveVoiceAction.MediaNext
        else -> null
    }

    private fun Intent.number(name: String): Number? = slots[name] as? Number

    private val supportedTemperatureRange =
        TemperatureUnits.MIN_CELSIUS.toDouble()..TemperatureUnits.MAX_CELSIUS.toDouble()
}
