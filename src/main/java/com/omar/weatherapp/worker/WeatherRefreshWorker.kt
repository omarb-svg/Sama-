package com.omar.weatherapp.worker

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.work.*
import com.omar.weatherapp.data.WeatherRepository
import com.omar.weatherapp.data.prefs.AppPreferences
import com.omar.weatherapp.widget.WeatherWidget
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WeatherRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs      = AppPreferences(applicationContext)
        val repository = WeatherRepository()
        val gson       = Gson()

        return try {
            val lat   = prefs.selectedLatitude.first()?.toDouble()  ?: return Result.success()
            val lon   = prefs.selectedLongitude.first()?.toDouble() ?: return Result.success()
            val tUnit = prefs.tempUnit.first()
            val wUnit = prefs.windUnit.first()
            val keys  = mapOf(
                "owm"  to prefs.owmApiKey.first(),
                "wapi" to prefs.wapiApiKey.first(),
                "vc"   to prefs.vcApiKey.first()
            )
            val provider = prefs.apiProvider.first()

            val state = repository.fetchWeather(lat, lon, tUnit, wUnit, provider, keys)
                .getOrNull() ?: return Result.retry()

            val name = prefs.locationName.first()
            prefs.saveCachedWeather(gson.toJson(state.copy(locationName = name)))

            // Refresh home-screen widget
            WeatherWidget().updateAll(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "kinetic_weather_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
