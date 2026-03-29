package com.omar.weatherapp.data.models

import com.google.gson.annotations.SerializedName

// ── Open-Meteo API response models ──────────────────────────────────────────

data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
    val current: CurrentWeatherData? = null,
    val hourly: HourlyData? = null,
    val daily: DailyData? = null
)

data class CurrentWeatherData(
    @SerializedName("temperature_2m") val temperature: Double = 0.0,
    @SerializedName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerializedName("relative_humidity_2m") val humidity: Int = 0,
    val precipitation: Double = 0.0,
    @SerializedName("weather_code") val weatherCode: Int = 0,
    @SerializedName("wind_speed_10m") val windSpeed: Double = 0.0,
    @SerializedName("wind_direction_10m") val windDirection: Int = 0,
    @SerializedName("surface_pressure") val pressure: Double = 0.0,
    val visibility: Double? = null,
    @SerializedName("uv_index") val uvIndex: Double? = null,
    @SerializedName("is_day") val isDay: Int = 1
)

data class HourlyData(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m") val temperatures: List<Double> = emptyList(),
    @SerializedName("apparent_temperature") val apparentTemperatures: List<Double> = emptyList(),
    @SerializedName("weather_code") val weatherCodes: List<Int> = emptyList(),
    @SerializedName("precipitation_probability") val precipProbabilities: List<Int?> = emptyList(),
    @SerializedName("wind_speed_10m") val windSpeeds: List<Double> = emptyList()
)

data class DailyData(
    val time: List<String> = emptyList(),
    @SerializedName("weather_code") val weatherCodes: List<Int> = emptyList(),
    @SerializedName("temperature_2m_max") val maxTemperatures: List<Double> = emptyList(),
    @SerializedName("temperature_2m_min") val minTemperatures: List<Double> = emptyList(),
    @SerializedName("apparent_temperature_max") val apparentMaxTemperatures: List<Double> = emptyList(),
    @SerializedName("apparent_temperature_min") val apparentMinTemperatures: List<Double> = emptyList(),
    @SerializedName("precipitation_probability_max") val precipProbabilities: List<Int?> = emptyList(),
    @SerializedName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList()
)

// ── Geocoding models ─────────────────────────────────────────────────────────

data class GeocodingResponse(
    val results: List<GeoLocation>? = null
)

data class GeoLocation(
    val id: Long = 0,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val admin2: String? = null
) {
    val displayName: String
        get() = buildString {
            append(name)
            if (!admin1.isNullOrBlank()) append(", $admin1")
            if (!country.isNullOrBlank()) append(", $country")
        }
}

// ── UI state models ──────────────────────────────────────────────────────────

data class WeatherUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val locationName: String = "Your Location",
    val isCurrentLocation: Boolean = false,
    val currentTemp: Int = 0,
    val feelsLike: Int = 0,
    val condition: String = "",
    val conditionCode: Int = 0,
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val windDirection: Int = 0,
    val pressure: Double = 0.0,
    val visibility: Double = 0.0,
    val uvIndex: Double = 0.0,
    val isDay: Boolean = true,
    val hourlyForecast: List<HourlyWeather> = emptyList(),
    val dailyForecast: List<DailyWeather> = emptyList(),
    val sunrise: String = "",
    val sunset: String = "",
    val tempUnit: String = "F",
    val windUnit: String = "mph"
)

data class HourlyWeather(
    val hour: String,
    val temp: Int,
    val feelsLike: Int,
    val conditionCode: Int,
    val precipProb: Int,
    val windSpeed: Double,
    val isCurrent: Boolean = false
)

data class DailyWeather(
    val dayName: String,
    val dateStr: String,
    val conditionCode: Int,
    val maxTemp: Int,
    val minTemp: Int,
    val feelsLikeMax: Int,
    val precipProb: Int,
    val uvIndex: Double,
    val isToday: Boolean = false
)

// ── Saved location for preferences ──────────────────────────────────────────

data class SavedLocation(
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

// ── Helper functions ─────────────────────────────────────────────────────────

fun weatherCodeToCondition(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1 -> "Mainly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45 -> "Foggy"
    48 -> "Icy Fog"
    51 -> "Light Drizzle"
    53 -> "Drizzle"
    55 -> "Heavy Drizzle"
    56 -> "Freezing Drizzle"
    57 -> "Heavy Freezing Drizzle"
    61 -> "Light Rain"
    63 -> "Rain"
    65 -> "Heavy Rain"
    66 -> "Freezing Rain"
    67 -> "Heavy Freezing Rain"
    71 -> "Light Snow"
    73 -> "Snow"
    75 -> "Heavy Snow"
    77 -> "Snow Grains"
    80 -> "Rain Showers"
    81 -> "Showers"
    82 -> "Heavy Showers"
    85 -> "Snow Showers"
    86 -> "Heavy Snow Showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm w/ Hail"
    99 -> "Heavy Thunderstorm"
    else -> "Unknown"
}

fun weatherCodeToEmoji(code: Int, isDay: Boolean = true): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1 -> if (isDay) "🌤️" else "🌙"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..57 -> "🌦️"
    in 61..67 -> "🌧️"
    in 71..77 -> "❄️"
    in 80..82 -> "🌦️"
    85, 86 -> "🌨️"
    in 95..99 -> "⛈️"
    else -> "🌡️"
}

fun windDirectionToText(degrees: Int): String {
    val dirs = listOf("N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW")
    val index = ((degrees + 11.25) / 22.5).toInt() % 16
    return dirs[index]
}
