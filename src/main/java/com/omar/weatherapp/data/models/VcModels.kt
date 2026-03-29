package com.omar.weatherapp.data.models

import com.google.gson.annotations.SerializedName

data class VcResponse(
    val latitude: Double,
    val longitude: Double,
    val resolvedAddress: String,
    val address: String,
    val timezone: String,
    val tzoffset: Double,
    val days: List<VcDay>,
    val currentConditions: VcCurrent?
)

data class VcDay(
    val datetime: String,
    val datetimeEpoch: Long,
    val tempmax: Double,
    val tempmin: Double,
    val temp: Double,
    val feelslikemax: Double,
    val feelslikemin: Double,
    val feelslike: Double,
    val dew: Double,
    val humidity: Double,
    val precip: Double,
    val precipprob: Double,
    val precipcover: Double,
    val snow: Double,
    val snowdepth: Double,
    val windspeed: Double,
    val winddir: Double,
    val pressure: Double,
    val visibility: Double,
    val cloudcover: Double,
    val uvindex: Double,
    val sunrise: String,
    val sunriseEpoch: Long,
    val sunset: String,
    val sunsetEpoch: Long,
    val moonphase: Double,
    val conditions: String,
    val icon: String,
    val hours: List<VcHour>?
)

data class VcHour(
    val datetime: String,
    val datetimeEpoch: Long,
    val temp: Double,
    val feelslike: Double,
    val humidity: Double,
    val precip: Double,
    val precipprob: Double,
    val snow: Double,
    val snowdepth: Double,
    val windspeed: Double,
    val winddir: Double,
    val pressure: Double,
    val visibility: Double,
    val cloudcover: Double,
    val uvindex: Double,
    val conditions: String,
    val icon: String
)

data class VcCurrent(
    val datetime: String,
    val datetimeEpoch: Long,
    val temp: Double,
    val feelslike: Double,
    val humidity: Double,
    val dew: Double,
    val precip: Double,
    val precipprob: Double,
    val snow: Double,
    val snowdepth: Double,
    val windspeed: Double,
    val winddir: Double,
    val pressure: Double,
    val visibility: Double,
    val cloudcover: Double,
    val uvindex: Double,
    val sunrise: String,
    val sunriseEpoch: Long,
    val sunset: String,
    val sunsetEpoch: Long,
    val moonphase: Double,
    val conditions: String,
    val icon: String
)
