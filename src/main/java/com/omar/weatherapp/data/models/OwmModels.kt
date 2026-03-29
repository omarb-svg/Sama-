package com.omar.weatherapp.data.models

import com.google.gson.annotations.SerializedName

data class OwmResponse(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val current: OwmCurrent?,
    val hourly: List<OwmHourly>?,
    val daily: List<OwmDaily>?
)

data class OwmCurrent(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("uvi") val uvIndex: Double,
    val visibility: Int,
    @SerializedName("wind_speed") val windSpeed: Double,
    @SerializedName("wind_deg") val windDeg: Int,
    val weather: List<OwmWeather>
)

data class OwmHourly(
    val dt: Long,
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("pop") val precipProb: Double,
    @SerializedName("wind_speed") val windSpeed: Double,
    val weather: List<OwmWeather>
)

data class OwmDaily(
    val dt: Long,
    val temp: OwmTemp,
    @SerializedName("feels_like") val feelsLike: OwmFeelsLike,
    @SerializedName("pop") val precipProb: Double,
    @SerializedName("uvi") val uvIndex: Double,
    val sunrise: Long,
    val sunset: Long,
    val weather: List<OwmWeather>
)

data class OwmTemp(val min: Double, val max: Double)
data class OwmFeelsLike(val day: Double, val night: Double, val eve: Double, val morn: Double)

data class OwmWeather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)
