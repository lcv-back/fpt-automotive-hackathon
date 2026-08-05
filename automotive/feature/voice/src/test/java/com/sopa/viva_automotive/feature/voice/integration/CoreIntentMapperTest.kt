package com.sopa.viva_automotive.feature.voice.integration

import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import com.viva.voice.intent.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreIntentMapperTest {

    @Test
    fun `climate temperature maps to vehicle intent`() {
        val action = CoreIntentMapper.map(intent("hvac_set_temp", "value" to 24f))

        assertEquals(
            AutomotiveVoiceAction.VehicleControl(VehicleIntent.SetTemperature(24.0)),
            action,
        )
    }

    @Test
    fun `maps all five backbone commands`() {
        val cases = mapOf(
            intent("hvac_set_temp", "value" to 24f) to AutomotiveVoiceAction.VehicleControl(
                VehicleIntent.SetTemperature(24.0),
            ),
            intent("hvac_set_fan", "level" to 2) to AutomotiveVoiceAction.VehicleControl(
                VehicleIntent.SetFanSpeed(2),
            ),
            intent("door_lock", "lock" to true) to AutomotiveVoiceAction.VehicleControl(
                VehicleIntent.SetDoorLock(true),
            ),
            intent("volume_adjust", "delta" to -1) to AutomotiveVoiceAction.VolumeAdjust(-1),
            intent("media_next") to AutomotiveVoiceAction.MediaNext,
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, CoreIntentMapper.map(input))
        }
    }

    @Test
    fun `missing or wrong slot returns null`() {
        assertEquals(null, CoreIntentMapper.map(intent("hvac_set_temp")))
        assertEquals(null, CoreIntentMapper.map(intent("door_lock", "lock" to "true")))
        assertEquals(null, CoreIntentMapper.map(intent("hvac_set_temp", "value" to Float.NaN)))
        assertEquals(null, CoreIntentMapper.map(intent("hvac_set_fan", "level" to 6)))
    }

    private fun intent(name: String, vararg slots: Pair<String, Any>) = Intent(
        name = name,
        slots = mapOf(*slots),
        confidence = 1f,
        tier = Intent.Tier.T0,
    )
}
