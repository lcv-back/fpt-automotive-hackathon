package com.sopa.viva_automotive.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sopa.viva_automotive.vehicleservice.impl.MockVehicleRepository
import com.sopa.viva_automotive.vehicleservice.impl.VstateCommands
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VehicleDummyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mockVehicle: MockVehicleRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_VSTATE) return

        val unitType = intent.getStringExtra(EXTRA_UNIT_TYPE)
        val stateValue = intent.getStringExtra(EXTRA_STATE_VALUE)

        VstateCommands.parse(unitType, stateValue)
            .onSuccess { write ->
                mockVehicle.injectVehicleEvent(write.propertyId, write.areaId, write.value)
                Log.i(TAG, "VSTATE $unitType=$stateValue -> property=${write.propertyId} area=${write.areaId} value=${write.value}")
            }
            .onFailure { error ->
                Log.w(TAG, "Rejected VSTATE command: ${error.message}")
            }
    }

    companion object {
        const val ACTION_VSTATE = "com.sopa.viva_automotive.mock.VSTATE"
        const val EXTRA_UNIT_TYPE = "unit_type"
        const val EXTRA_STATE_VALUE = "state_value"
        private const val TAG = "VehicleDummyReceiver"
    }
}
