package com.sopa.viva_automotive.feature.voice.integration

import com.sopa.viva_automotive.feature.voice.domain.CommandNotWiredException
import com.sopa.viva_automotive.feature.voice.domain.CommandValidationException
import com.sopa.viva_automotive.feature.voice.domain.ExecuteVehicleControlUseCase
import com.viva.voice.agent.CommandGateway
import com.viva.voice.agent.CommandResult
import com.viva.voice.intent.Intent
import com.viva.voice.trace.LatencyTrace
import com.viva.voice.trace.Stage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCommandGateway @Inject constructor(
    private val executeVehicleControl: ExecuteVehicleControlUseCase,
) : CommandGateway {

    override suspend fun execute(intent: Intent, trace: LatencyTrace): CommandResult {
        val action = CoreIntentMapper.map(intent)
            ?: return CommandResult.Failed("No adapter for intent \"${intent.name}\"")

        val vehicleIntent = when (action) {
            is AutomotiveVoiceAction.VehicleControl -> action.intent
            is AutomotiveVoiceAction.VolumeAdjust ->
                com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent.VolumeAdjust(action.delta)
            AutomotiveVoiceAction.MediaNext ->
                com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent.MediaNext
        }

        return executeVehicleControl(vehicleIntent).fold(
            onSuccess = { spoken ->
                trace.mark(Stage.EXEC_DONE)
                CommandResult.Applied(spokenVi = spoken)
            },
            onFailure = { error ->
                when (error) {
                    is CommandValidationException ->
                        CommandResult.Failed(error.message ?: "validation failed")
                    is CommandNotWiredException ->
                        CommandResult.Failed(error.message ?: "not wired")
                    else -> CommandResult.Failed(error.message ?: "execution failed")
                }
            },
        )
    }
}
