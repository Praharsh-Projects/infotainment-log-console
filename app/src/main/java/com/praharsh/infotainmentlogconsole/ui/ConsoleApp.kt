package com.praharsh.infotainmentlogconsole.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.praharsh.infotainmentlogconsole.ui.theme.LogConsoleTheme

private data class Destination(
    val route: String,
    val label: String,
    val shortLabel: String
)

private val destinations = listOf(
    Destination(route = "live", label = "Live", shortLabel = "L"),
    Destination(route = "saved", label = "Saved", shortLabel = "S"),
    Destination(route = "settings", label = "Settings", shortLabel = "A")
)

@Composable
fun InfotainmentLogConsoleApp(
    viewModel: LogConsoleViewModel,
    apiBaseUrl: String
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: destinations.first().route

    LogConsoleTheme(activeBrandKey = state.activeBrandKey) {
        Surface {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        destinations.forEach { destination ->
                            NavigationBarItem(
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
                                icon = { Text(destination.shortLabel) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = destinations.first().route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("live") {
                        LiveLogsScreen(state = state, viewModel = viewModel)
                    }
                    composable("saved") {
                        SavedLogsScreen(state = state, viewModel = viewModel)
                    }
                    composable("settings") {
                        SettingsScreen(
                            state = state,
                            apiBaseUrl = apiBaseUrl,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
