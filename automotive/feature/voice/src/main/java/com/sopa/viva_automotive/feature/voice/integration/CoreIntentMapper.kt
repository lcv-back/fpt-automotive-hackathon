package com.sopa.viva_automotive.feature.voice.integration

import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import com.viva.voice.intent.Intent

/** Action understood by Dương's app layer after Long's voice core has routed text. */
sealed interface AutomotiveVoiceAction {
    data class VehicleControl(val intent: VehicleIntent) : AutomotiveVoiceAction
    data class VolumeAdjust(val delta: Int) : AutomotiveVoiceAction
    data object MediaNext : AutomotiveVoiceAction
}

/**
 * The only type translation between `:voice-core` and `:feature:voice`.
 *
 * Returning null is deliberate: malformed slots stop at the module boundary
 * instead of becoming a default vehicle command.
 */
object CoreIntentMapper {
    fun map(intent: Intent): AutomotiveVoiceAction? = when (intent.name) {
        "hvac_set_temp" -> intent.number("value")
            ?.toDouble()
            ?.takeIf { value -> value.isFinite() && value in MIN_TEMPERATURE_C..MAX_TEMPERATURE_C }
            ?.let { value ->
                AutomotiveVoiceAction.VehicleControl(
                    VehicleIntent.SetTemperature(value),
                )
            }

        "hvac_set_fan" -> intent.number("level")
            ?.toDouble()
            ?.takeIf { level -> level % 1.0 == 0.0 && level in MIN_FAN_LEVEL..MAX_FAN_LEVEL }
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

    private const val MIN_TEMPERATURE_C = 16.0
    private const val MAX_TEMPERATURE_C = 32.0
    private const val MIN_FAN_LEVEL = 0.0
    private const val MAX_FAN_LEVEL = 5.0
}
