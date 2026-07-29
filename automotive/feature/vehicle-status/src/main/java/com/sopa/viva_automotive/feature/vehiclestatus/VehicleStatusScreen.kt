package com.sopa.viva_automotive.feature.vehiclestatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sopa.viva_automotive.core.ui.components.SectionCard
import com.sopa.viva_automotive.core.ui.components.StatusTile
import com.sopa.viva_automotive.core.ui.components.VivaToggleRow
import kotlin.math.roundToInt

@Composable
fun VehicleStatusScreen(
    modifier: Modifier = Modifier,
    viewModel: VehicleStatusViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.status_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatusTile(
                icon = Icons.Default.Speed,
                label = stringResource(R.string.status_speed),
                value = stringResource(
                    R.string.status_speed_value,
                    status.speedKmh.roundToInt(),
                ),
                modifier = Modifier.weight(1f),
            )
            StatusTile(
                icon = Icons.Default.LocalGasStation,
                label = stringResource(R.string.status_fuel),
                value = stringResource(
                    R.string.status_fuel_value,
                    status.fuelLevelPercent.roundToInt(),
                ),
                alert = status.fuelLevelPercent < 15f,
                modifier = Modifier.weight(1f),
            )
            StatusTile(
                icon = Icons.Default.BatteryChargingFull,
                label = stringResource(R.string.status_battery),
                value = stringResource(
                    R.string.status_battery_value,
                    status.batteryLevelPercent.roundToInt(),
                ),
                alert = status.batteryLevelPercent < 15f,
                modifier = Modifier.weight(1f),
            )
            StatusTile(
                icon = when {
                    status.driverDoorOpen -> Icons.Default.MeetingRoom
                    status.doorsLocked -> Icons.Default.Lock
                    else -> Icons.Default.LockOpen
                },
                label = stringResource(R.string.status_doors),
                value = stringResource(
                    when {
                        status.driverDoorOpen -> R.string.status_doors_open
                        status.doorsLocked -> R.string.status_doors_locked
                        else -> R.string.status_doors_unlocked
                    },
                ),
                alert = status.driverDoorOpen || !status.doorsLocked,
                modifier = Modifier.weight(1f),
            )
        }

        SectionCard(title = stringResource(R.string.status_body_controls)) {
            VivaToggleRow(
                label = stringResource(R.string.status_doors_locked_toggle),
                checked = status.doorsLocked,
                onCheckedChange = viewModel::setDoorsLocked,
                icon = Icons.Default.Lock,
            )
            VivaToggleRow(
                label = stringResource(R.string.status_interior_lights),
                checked = status.cabinLightsOn,
                onCheckedChange = viewModel::setCabinLights,
                icon = Icons.Default.Lightbulb,
            )
        }
    }
}
