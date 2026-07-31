package com.sopa.viva_automotive.feature.hvac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sopa.viva_automotive.core.common.units.TemperatureUnits
import com.sopa.viva_automotive.core.ui.components.SectionCard
import com.sopa.viva_automotive.core.ui.components.ValueStepper
import com.sopa.viva_automotive.core.ui.components.VivaToggleRow
import com.sopa.viva_automotive.core.ui.theme.VivaDimens
import com.sopa.viva_automotive.vehicleservice.api.FanDirection
import com.sopa.viva_automotive.vehicleservice.api.VehicleZone

private val fanDirectionOptions = listOf(
    FanDirection.FACE to R.string.hvac_fan_face,
    FanDirection.FLOOR to R.string.hvac_fan_floor,
    FanDirection.FACE_AND_FLOOR to R.string.hvac_fan_both,
    FanDirection.DEFROST to R.string.hvac_fan_defrost,
)

@Composable
fun HvacScreen(
    modifier: Modifier = Modifier,
    viewModel: HvacViewModel = hiltViewModel(),
) {
    val climate by viewModel.climate.collectAsStateWithLifecycle()
    val useFahrenheit by viewModel.useFahrenheit.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.errors.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.hvac_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SectionCard(
                    title = stringResource(R.string.hvac_driver),
                    modifier = Modifier.weight(1f),
                ) {
                    ValueStepper(
                        label = stringResource(R.string.hvac_temperature),
                        value = TemperatureUnits.format(climate.driverTempCelsius, useFahrenheit),
                        onIncrement = { viewModel.adjustTemperature(VehicleZone.DRIVER, increase = true) },
                        onDecrement = { viewModel.adjustTemperature(VehicleZone.DRIVER, increase = false) },
                        enabled = climate.hvacPowerOn,
                    )
                }
                SectionCard(
                    title = stringResource(R.string.hvac_passenger),
                    modifier = Modifier.weight(1f),
                ) {
                    ValueStepper(
                        label = stringResource(R.string.hvac_temperature),
                        value = TemperatureUnits.format(climate.passengerTempCelsius, useFahrenheit),
                        onIncrement = {
                            viewModel.adjustTemperature(VehicleZone.PASSENGER, increase = true)
                        },
                        onDecrement = {
                            viewModel.adjustTemperature(VehicleZone.PASSENGER, increase = false)
                        },
                        enabled = climate.hvacPowerOn,
                    )
                }
            }

            SectionCard(title = stringResource(R.string.hvac_airflow)) {
                ValueStepper(
                    label = stringResource(R.string.hvac_fan_speed),
                    value = climate.fanSpeed.toString(),
                    onIncrement = { viewModel.adjustFanSpeed(+1) },
                    onDecrement = { viewModel.adjustFanSpeed(-1) },
                    enabled = climate.hvacPowerOn,
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(VivaDimens.ButtonHeight),
                ) {
                    fanDirectionOptions.forEachIndexed { index, (direction, labelRes) ->
                        SegmentedButton(
                            selected = climate.fanDirection == direction,
                            onClick = { viewModel.setFanDirection(direction) },
                            enabled = climate.hvacPowerOn,
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = fanDirectionOptions.size,
                            ),
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
                VivaToggleRow(
                    label = stringResource(R.string.hvac_ac),
                    checked = climate.acOn,
                    onCheckedChange = viewModel::setAc,
                    icon = Icons.Default.AcUnit,
                    enabled = climate.hvacPowerOn,
                )
                VivaToggleRow(
                    label = stringResource(R.string.hvac_auto),
                    checked = climate.autoOn,
                    onCheckedChange = viewModel::setAuto,
                    icon = Icons.Default.Autorenew,
                    enabled = climate.hvacPowerOn,
                )
                VivaToggleRow(
                    label = stringResource(R.string.hvac_power),
                    checked = climate.hvacPowerOn,
                    onCheckedChange = viewModel::setHvacPower,
                    icon = Icons.Default.PowerSettingsNew,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
