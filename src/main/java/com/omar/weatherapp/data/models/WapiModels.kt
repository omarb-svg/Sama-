package com.omar.weatherapp.data.models

import com.google.gson.annotations.SerializedName

data class WapiResponse(
    val location: WapiLocation,
    val current: WapiCurrent,
    val forecast: WapiForecast
)

data class WapiLocation(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val localtime_epoch: Long
)

data class WapiCurrent(
    val last_updated_epoch: Long,
    val temp_c: Double,
    val temp_f: Double,
    val is_day: Int,
    val condition: WapiCondition,
    val wind_mph: Double,
    val wind_kph: Double,
    val wind_degree: Int,
    val pressure_mb: Double,
    val humidity: Int,
    val feelslike_c: Double,
    val feelslike_f: Double,
    val vis_km: Double,
    val vis_miles: Double,
    val uv: Double
)

data class WapiForecast(
    val forecastday: List<WapiForecastDay>
)

data class WapiForecastDay(
    val date: String,
    val date_epoch: Long,
    val day: WapiDay,
    val astro: WapiAstro,
    val hour: List<WapiHour>
)

data class WapiDay(
    val maxtemp_c: Double,
    val maxtemp_f: Double,
    val mintemp_c: Double,
    val mintemp_f: Double,
    val avgtemp_c: Double,
    val avgtemp_f: Double,
    val maxwind_mph: Double,
    val maxwind_kph: Double,
    val totalsnow_cm: Double,
    val daily_will_it_rain: Int,
    val daily_chance_of_rain: Int,
    val daily_will_it_snow: Int,
    val daily_chance_of_snow: Int,
    val condition: WapiCondition,
    val uv: Double
)

data class WapiHour(
    val time_epoch: Long,
    val temp_c: Double,
    val temp_f: Double,
    val condition: WapiCondition,
    val wind_mph: Double,
    val wind_kph: Double,
    val wind_degree: Int,
    val pressure_mb: Double,
    val humidity: Int,
    val feelslike_c: Double,
    val feelslike_f: Double,
    val chance_of_rain: Int,
    val chance_of_snow: Int,
    val is_day: Int
)

data class WapiAstro(
    val sunrise: String,
    val sunset: String
)

data class WapiCondition(
    val text: String,
    val icon: String,
    val code: Int
)
