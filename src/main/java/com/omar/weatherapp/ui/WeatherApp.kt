package com.omar.weatherapp.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omar.weatherapp.WeatherViewModel
import com.omar.weatherapp.ui.screens.SettingsScreen
import com.omar.weatherapp.ui.screens.WeatherScreen

object Routes {
    const val WEATHER  = "weather"
    const val SETTINGS = "settings"
}

@Composable
fun WeatherApp(viewModel: WeatherViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.WEATHER) {

        composable(Routes.WEATHER) {
            WeatherScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
