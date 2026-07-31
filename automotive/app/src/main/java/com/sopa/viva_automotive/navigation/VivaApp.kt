package com.sopa.viva_automotive.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sopa.viva_automotive.R
import com.sopa.viva_automotive.feature.hvac.HvacScreen
import com.sopa.viva_automotive.feature.settings.SettingsScreen
import com.sopa.viva_automotive.feature.vehiclestatus.VehicleStatusScreen
import com.sopa.viva_automotive.feature.voice.presentation.VoiceOverlay

private enum class VivaDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HVAC("hvac", R.string.nav_climate, Icons.Default.Thermostat),
    STATUS("status", R.string.nav_vehicle, Icons.Default.DirectionsCar),
    SETTINGS("settings", R.string.nav_settings, Icons.Default.Settings),
}

@Composable
fun VivaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            VivaDestination.entries.forEach { destination ->
                val label = stringResource(destination.labelRes)
                NavigationRailItem(
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(destination.icon, contentDescription = label) },
                    label = { Text(label) },
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = VivaDestination.HVAC.route,
                modifier = Modifier.weight(1f),
            ) {
                composable(VivaDestination.HVAC.route) { HvacScreen() }
                composable(VivaDestination.STATUS.route) { VehicleStatusScreen() }
                composable(VivaDestination.SETTINGS.route) { SettingsScreen() }
            }

            VoiceOverlay(modifier = Modifier.padding(16.dp))
        }
    }
}
