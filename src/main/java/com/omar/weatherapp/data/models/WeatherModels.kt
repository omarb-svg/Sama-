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
    @SerializedName("is_day") val isDay: Int = 1,
    @SerializedName("cloud_cover") val cloudCover: Int = 0
)

data class HourlyData(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m") val temperatures: List<Double> = emptyList(),
    @SerializedName("apparent_temperature") val apparentTemperatures: List<Double> = emptyList(),
    @SerializedName("weather_code") val weatherCodes: List<Int> = emptyList(),
    @SerializedName("precipitation_probability") val precipProbabilities: List<Int?> = emptyList(),
    @SerializedName("wind_speed_10m") val windSpeeds: List<Double> = emptyList(),
    @SerializedName("wind_direction_10m") val windDirections: List<Int> = emptyList(),
    @SerializedName("cloud_cover") val cloudCover: List<Int> = emptyList(),
    @SerializedName("dew_point_2m") val dewPoints: List<Double> = emptyList(),
    @SerializedName("surface_pressure") val pressures: List<Double> = emptyList(),
    @SerializedName("visibility") val visibilities: List<Double> = emptyList(),
    @SerializedName("uv_index") val uvIndices: List<Double> = emptyList()
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
    val pressure: Double = 0.0,          // hPa
    val pressureInHg: Double = 0.0,      // inHg (US display)
    val visibility: Double = 0.0,        // km or miles depending on unit
    val uvIndex: Double = 0.0,
    val isDay: Boolean = true,
    val cloudCover: Int = 0,             // percent 0-100
    val dewPoint: Int = 0,               // °F or °C
    val moonPhase: String = "",          // e.g. "WXG"
    val precipStatus: String = "NONE",   // NONE / TRACE / LIGHT / HEAVY
    val hourlyForecast: List<HourlyWeather> = emptyList(),
    val dailyForecast: List<DailyWeather> = emptyList(),
    val sunrise: String = "",
    val sunset: String = "",
    val tempUnit: String = "F",
    val windUnit: String = "mph"
)

data class HourlyWeather(
    val hour: String,
    val hourIndex: Int = 0,       // 0-23 index into today's hours
    val temp: Int,
    val feelsLike: Int,
    val conditionCode: Int,
    val precipProb: Int,
    val windSpeed: Double,
    val windDirection: Int = 0,
    val cloudCover: Int = 0,
    val dewPoint: Int = 0,
    val pressure: Double = 0.0,
    val uvIndex: Double = 0.0,
    val visibility: Double = 0.0,
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

/**
 * Calculates current moon phase abbreviation using astronomical cycle.
 * Reference new moon: Jan 6, 2000 (J2000 epoch).
 */
fun calculateMoonPhaseAbbr(): String {
    val knownNewMoon = java.time.LocalDate.of(2000, 1, 6)
    val today = java.time.LocalDate.now()
    val daysSince = java.time.temporal.ChronoUnit.DAYS.between(knownNewMoon, today).toDouble()
    val cycle = 29.53059
    val phase = ((daysSince % cycle) + cycle) % cycle
    return when {
        phase < 1.85  -> "NM"   // New Moon
        phase < 7.38  -> "WXC"  // Waxing Crescent
        phase < 9.22  -> "FQ"   // First Quarter
        phase < 14.77 -> "WXG"  // Waxing Gibbous
        phase < 16.61 -> "FM"   // Full Moon
        phase < 22.15 -> "WNG"  // Waning Gibbous
        phase < 23.99 -> "LQ"   // Last Quarter
        else          -> "WNC"  // Waning Crescent
    }
}

fun moonPhaseFromString(wapi: String): String = when {
    wapi.contains("New", true)             -> "NM"
    wapi.contains("Waxing Crescent", true) -> "WXC"
    wapi.contains("First Quarter", true)   -> "FQ"
    wapi.contains("Waxing Gibbous", true)  -> "WXG"
    wapi.contains("Full", true)            -> "FM"
    wapi.contains("Waning Gibbous", true)  -> "WNG"
    wapi.contains("Last Quarter", true)    -> "LQ"
    wapi.contains("Waning Crescent", true) -> "WNC"
    else -> calculateMoonPhaseAbbr()
}
