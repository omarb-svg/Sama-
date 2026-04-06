package com.omar.weatherapp.data

import com.omar.weatherapp.data.api.GeocodingService
import com.omar.weatherapp.data.api.OpenMeteoService
import com.omar.weatherapp.data.api.OwmService
import com.omar.weatherapp.data.api.VcService
import com.omar.weatherapp.data.api.WapiService
import com.omar.weatherapp.data.models.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.temporal.ChronoUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class WeatherRepository {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private val openMeteoService: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoService.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoService::class.java)
    }

    private val geocodingService: GeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl(GeocodingService.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingService::class.java)
    }

    private val owmService: OwmService by lazy {
        Retrofit.Builder()
            .baseUrl(OwmService.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OwmService::class.java)
    }

    private val wapiService: WapiService by lazy {
        Retrofit.Builder()
            .baseUrl(WapiService.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WapiService::class.java)
    }

    private val vcService: VcService by lazy {
        Retrofit.Builder()
            .baseUrl(VcService.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VcService::class.java)
    }

    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        tempUnit: String = "F",
        windUnit: String = "mph",
        provider: String = "open_meteo",
        keys: Map<String, String> = emptyMap()
    ): Result<WeatherUiState> {
        return try {
            when (provider) {
                "openweathermap" -> {
                    val key = keys["owm"] ?: ""
                    val units = if (tempUnit == "F") "imperial" else "metric"
                    val response = owmService.getOneCall(latitude, longitude, key, units)
                    Result.success(mapOwmToUiState(response, tempUnit, windUnit))
                }
                "weatherapi" -> {
                    val key = keys["wapi"] ?: ""
                    val response = wapiService.getForecast(key, "$latitude,$longitude")
                    Result.success(mapWapiToUiState(response, tempUnit, windUnit))
                }
                "visualcrossing" -> {
                    val key = keys["vc"] ?: ""
                    val units = if (tempUnit == "F") "us" else "metric"
                    val response = vcService.getTimeline("$latitude,$longitude", key, units)
                    Result.success(mapVcToUiState(response, tempUnit, windUnit))
                }
                "average_consensus" -> {
                    fetchConsensusWeather(latitude, longitude, tempUnit, windUnit, keys)
                }
                else -> { // open_meteo
                    val tUnitParam = if (tempUnit == "F") "fahrenheit" else "celsius"
                    val wUnitParam = if (windUnit == "kmh") "kmh" else if (windUnit == "ms") "ms" else "mph"
                    val response = openMeteoService.getForecast(latitude, longitude, temperatureUnit = tUnitParam, windSpeedUnit = wUnitParam)
                    Result.success(mapToUiState(response, tempUnit, windUnit))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchConsensusWeather(
        lat: Double,
        lon: Double,
        tempUnit: String,
        windUnit: String,
        keys: Map<String, String>
    ): Result<WeatherUiState> = coroutineScope {
        val jobs = listOf(
            async { runCatching { 
                val tUnitParam = if (tempUnit == "F") "fahrenheit" else "celsius"
                val wUnitParam = if (windUnit == "kmh") "kmh" else if (windUnit == "ms") "ms" else "mph"
                mapToUiState(openMeteoService.getForecast(lat, lon, temperatureUnit = tUnitParam, windSpeedUnit = wUnitParam), tempUnit, windUnit)
            }.getOrNull() },
            async { runCatching {
                val key = keys["owm"] ?: ""
                val units = if (tempUnit == "F") "imperial" else "metric"
                mapOwmToUiState(owmService.getOneCall(lat, lon, key, units), tempUnit, windUnit)
            }.getOrNull() },
            async { runCatching {
                val key = keys["wapi"] ?: ""
                mapWapiToUiState(wapiService.getForecast(key, "$lat,$lon"), tempUnit, windUnit)
            }.getOrNull() },
            async { runCatching {
                val key = keys["vc"] ?: ""
                val units = if (tempUnit == "F") "us" else "metric"
                mapVcToUiState(vcService.getTimeline("$lat,$lon", key, units), tempUnit, windUnit)
            }.getOrNull() }
        )

        val results = jobs.awaitAll().filterNotNull()
        if (results.isEmpty()) return@coroutineScope Result.failure(Exception("All weather providers failed"))

        val consensus = WeatherUiState(
            isLoading = false,
            currentTemp = results.map { it.currentTemp }.average().toInt(),
            feelsLike = results.map { it.feelsLike }.average().toInt(),
            humidity = results.map { it.humidity }.average().toInt(),
            windSpeed = results.map { it.windSpeed }.average(),
            uvIndex = results.map { it.uvIndex }.average(),
            condition = results.groupBy { it.condition }.maxByOrNull { it.value.size }?.key ?: "Unknown",
            conditionCode = results.groupBy { it.conditionCode }.maxByOrNull { it.value.size }?.key ?: 0,
            isDay = results.count { it.isDay } > results.size / 2,
            tempUnit = tempUnit,
            windUnit = windUnit,
            hourlyForecast = results.first().hourlyForecast,
            dailyForecast = results.first().dailyForecast
        )
        Result.success(consensus)
    }

    suspend fun searchLocations(query: String): Result<List<GeoLocation>> {
        return try {
            val response = geocodingService.searchLocation(query)
            Result.success(response.results ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToUiState(
        response: OpenMeteoResponse,
        tempUnit: String,
        windUnit: String
    ): WeatherUiState {
        val current = response.current ?: return WeatherUiState()
        val hourly  = response.hourly
        val daily   = response.daily

        // ── Hourly: 24 hours starting from current hour ───────────────────
        val nowStr     = LocalDateTime.now()
        val hourlyList = mutableListOf<HourlyWeather>()
        val isMph      = windUnit == "mph"
        val isFah      = tempUnit == "F"

        if (hourly != null) {
            val nowHour = nowStr.withMinute(0).withSecond(0).withNano(0)
            val dtFmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

            var foundCurrent = false
            for (i in hourly.time.indices) {
                runCatching {
                    val t = LocalDateTime.parse(hourly.time[i], dtFmt)
                    if (t >= nowHour && hourlyList.size < 24) {
                        val isCurrent = !foundCurrent && t == nowHour
                        if (isCurrent) foundCurrent = true

                        val hourLabel = when {
                            isCurrent -> "Now"
                            t.hour == 0 -> "12 AM"
                            t.hour < 12 -> "${t.hour} AM"
                            t.hour == 12 -> "12 PM"
                            else -> "${t.hour - 12} PM"
                        }

                        val rawVis = hourly.visibilities.getOrNull(i) ?: 0.0
                        val hourVis = if (isMph) rawVis / 1609.34 else rawVis / 1000.0

                        hourlyList.add(
                            HourlyWeather(
                                hour = hourLabel,
                                hourIndex = t.hour,
                                temp = hourly.temperatures.getOrNull(i)?.toInt() ?: 0,
                                feelsLike = hourly.apparentTemperatures.getOrNull(i)?.toInt() ?: 0,
                                conditionCode = hourly.weatherCodes.getOrNull(i) ?: 0,
                                precipProb = hourly.precipProbabilities.getOrNull(i) ?: 0,
                                windSpeed = hourly.windSpeeds.getOrNull(i) ?: 0.0,
                                windDirection = hourly.windDirections.getOrNull(i) ?: 0,
                                cloudCover = hourly.cloudCover.getOrNull(i) ?: 0,
                                dewPoint = hourly.dewPoints.getOrNull(i)?.toInt() ?: 0,
                                pressure = hourly.pressures.getOrNull(i) ?: 0.0,
                                uvIndex = hourly.uvIndices.getOrNull(i) ?: 0.0,
                                visibility = hourVis,
                                isCurrent = isCurrent
                            )
                        )
                    }
                }
            }
        }

        // ── Daily forecast ────────────────────────────────────────────────
        val dailyList = mutableListOf<DailyWeather>()
        val today = LocalDate.now()

        if (daily != null) {
            val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            for (i in daily.time.indices) {
                runCatching {
                    val date = LocalDate.parse(daily.time[i], dateFmt)
                    val dayName = when {
                        date == today -> "Today"
                        date == today.plusDays(1) -> "Tomorrow"
                        else -> date.dayOfWeek.name.take(3).lowercase()
                            .replaceFirstChar { it.uppercase() }
                    }
                    dailyList.add(
                        DailyWeather(
                            dayName = dayName,
                            dateStr = daily.time[i],
                            conditionCode = daily.weatherCodes.getOrNull(i) ?: 0,
                            maxTemp = daily.maxTemperatures.getOrNull(i)?.toInt() ?: 0,
                            minTemp = daily.minTemperatures.getOrNull(i)?.toInt() ?: 0,
                            feelsLikeMax = daily.apparentMaxTemperatures.getOrNull(i)?.toInt() ?: 0,
                            precipProb = daily.precipProbabilities.getOrNull(i) ?: 0,
                            uvIndex = daily.uvIndexMax.getOrNull(i) ?: 0.0,
                            isToday = date == today
                        )
                    )
                }
            }
        }

        // ── Sunrise/Sunset ────────────────────────────────────────────────
        val sunriseRaw = daily?.sunrise?.getOrNull(0) ?: ""
        val sunsetRaw  = daily?.sunset?.getOrNull(0) ?: ""
        val sunFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val outFmt = DateTimeFormatter.ofPattern("h:mm a")

        val sunriseFormatted = runCatching {
            LocalDateTime.parse(sunriseRaw, sunFmt).format(outFmt)
        }.getOrDefault(sunriseRaw)

        val sunsetFormatted = runCatching {
            LocalDateTime.parse(sunsetRaw, sunFmt).format(outFmt)
        }.getOrDefault(sunsetRaw)

        val isMphFinal  = windUnit == "mph"
        val rawVis      = current.visibility ?: 0.0
        val visConverted = if (isMphFinal) rawVis / 1609.34 else rawVis / 1000.0
        val presHg       = current.pressure / 33.8639  // hPa → inHg
        // Dew point: Magnus approximation T - ((100 - RH) / 5)
        val dewPtRaw = current.temperature - ((100 - current.humidity) / 5.0)
        val dewPt    = dewPtRaw.toInt()

        val moonPhase = if (daily?.sunrise?.isNotEmpty() == true) {
            calculateMoonPhaseAbbr()
        } else calculateMoonPhaseAbbr()

        val precipStatus = when {
            current.precipitation > 5.0  -> "HEAVY"
            current.precipitation > 1.0  -> "LIGHT"
            current.precipitation > 0.0  -> "TRACE"
            else                          -> "NONE"
        }

        return WeatherUiState(
            isLoading = false,
            error = null,
            currentTemp = current.temperature.toInt(),
            feelsLike = current.apparentTemperature.toInt(),
            condition = weatherCodeToCondition(current.weatherCode),
            conditionCode = current.weatherCode,
            humidity = current.humidity,
            windSpeed = current.windSpeed,
            windDirection = current.windDirection,
            pressure = current.pressure,
            pressureInHg = String.format("%.1f", presHg).toDouble(),
            visibility = visConverted,
            uvIndex = current.uvIndex ?: 0.0,
            isDay = current.isDay == 1,
            cloudCover = current.cloudCover,
            dewPoint = dewPt,
            moonPhase = moonPhase,
            precipStatus = precipStatus,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            sunrise = sunriseFormatted,
            sunset = sunsetFormatted,
            tempUnit = tempUnit,
            windUnit = windUnit
        )
    }

    private fun mapOwmToUiState(response: OwmResponse, tempUnit: String, windUnit: String): WeatherUiState {
        val current = response.current ?: return WeatherUiState()
        return WeatherUiState(
            isLoading = false,
            currentTemp = current.temp.toInt(),
            feelsLike = current.feelsLike.toInt(),
            condition = current.weather.firstOrNull()?.main ?: "Clear",
            conditionCode = owmToWmoCode(current.weather.firstOrNull()?.id ?: 800),
            humidity = current.humidity,
            windSpeed = current.windSpeed,
            windDirection = current.windDeg,
            pressure = current.pressure.toDouble(),
            visibility = current.visibility / 1000.0,
            uvIndex = current.uvIndex,
            isDay = current.dt > current.sunrise && current.dt < current.sunset,
            tempUnit = tempUnit,
            windUnit = windUnit,
            hourlyForecast = response.hourly?.take(24)?.map { 
                HourlyWeather(
                    hour = LocalDateTime.ofEpochSecond(it.dt, 0, java.time.ZoneOffset.UTC).hour.let { h -> if (h == 0) "12 AM" else if (h < 12) "$h AM" else if (h == 12) "12 PM" else "${h-12} PM" },
                    temp = it.temp.toInt(),
                    feelsLike = it.feelsLike.toInt(),
                    conditionCode = owmToWmoCode(it.weather.firstOrNull()?.id ?: 800),
                    precipProb = (it.precipProb * 100).toInt(),
                    windSpeed = it.windSpeed
                )
            } ?: emptyList()
        )
    }

    private fun mapWapiToUiState(response: WapiResponse, tempUnit: String, windUnit: String): WeatherUiState {
        val current = response.current
        val astro = response.forecast.forecastday.firstOrNull()?.astro
        val moonPh = astro?.moon_phase?.let { moonPhaseFromString(it) } ?: calculateMoonPhaseAbbr()
        val dewRaw = (if (tempUnit == "F") current.temp_f else current.temp_c) - ((100 - current.humidity) / 5.0)
        return WeatherUiState(
            isLoading = false,
            currentTemp = if (tempUnit == "F") current.temp_f.toInt() else current.temp_c.toInt(),
            feelsLike = if (tempUnit == "F") current.feelslike_f.toInt() else current.feelslike_c.toInt(),
            condition = current.condition.text,
            conditionCode = wapiToWmoCode(current.condition.code),
            humidity = current.humidity,
            windSpeed = if (windUnit == "mph") current.wind_mph else current.wind_kph,
            windDirection = current.wind_degree,
            pressure = current.pressure_mb,
            pressureInHg = current.pressure_mb / 33.8639,
            visibility = if (windUnit == "mph") current.vis_miles else current.vis_km,
            uvIndex = current.uv,
            isDay = current.is_day == 1,
            dewPoint = dewRaw.toInt(),
            moonPhase = moonPh,
            sunrise = astro?.sunrise ?: "",
            sunset = astro?.sunset ?: "",
            tempUnit = tempUnit,
            windUnit = windUnit,
            hourlyForecast = response.forecast.forecastday.firstOrNull()?.hour?.take(24)?.map {
                HourlyWeather(
                    hour = LocalDateTime.ofEpochSecond(it.time_epoch, 0, java.time.ZoneOffset.UTC).hour.let { h -> if (h == 0) "12 AM" else if (h < 12) "$h AM" else if (h == 12) "12 PM" else "${h-12} PM" },
                    temp = if (tempUnit == "F") it.temp_f.toInt() else it.temp_c.toInt(),
                    feelsLike = if (tempUnit == "F") it.feelslike_f.toInt() else it.feelslike_c.toInt(),
                    conditionCode = wapiToWmoCode(it.condition.code),
                    precipProb = it.chance_of_rain,
                    windSpeed = if (windUnit == "mph") it.wind_mph else it.wind_kph
                )
            } ?: emptyList()
        )
    }

    private fun mapVcToUiState(response: VcResponse, tempUnit: String, windUnit: String): WeatherUiState {
        val current = response.currentConditions ?: return WeatherUiState()
        val today = response.days.firstOrNull()
        return WeatherUiState(
            isLoading = false,
            currentTemp = current.temp.toInt(),
            feelsLike = current.feelslike.toInt(),
            condition = current.conditions,
            conditionCode = vcIconToWmoCode(current.icon),
            humidity = current.humidity.toInt(),
            windSpeed = current.windspeed,
            windDirection = current.winddir.toInt(),
            pressure = current.pressure,
            visibility = current.visibility,
            uvIndex = current.uvindex,
            isDay = current.datetimeEpoch > (today?.sunriseEpoch ?: 0) && current.datetimeEpoch < (today?.sunsetEpoch ?: 0),
            tempUnit = tempUnit,
            windUnit = windUnit,
            hourlyForecast = today?.hours?.take(24)?.map {
                HourlyWeather(
                    hour = LocalDateTime.ofEpochSecond(it.datetimeEpoch, 0, java.time.ZoneOffset.UTC).hour.let { h -> if (h == 0) "12 AM" else if (h < 12) "$h AM" else if (h == 12) "12 PM" else "${h-12} PM" },
                    temp = it.temp.toInt(),
                    feelsLike = it.feelslike.toInt(),
                    conditionCode = vcIconToWmoCode(it.icon),
                    precipProb = it.precipprob.toInt(),
                    windSpeed = it.windspeed
                )
            } ?: emptyList()
        )
    }

    private fun owmToWmoCode(owmCode: Int): Int = when (owmCode) {
        in 200..232 -> 95 // Thunderstorm
        in 300..321 -> 51 // Drizzle
        in 500..531 -> 61 // Rain
        in 600..622 -> 71 // Snow
        in 701..781 -> 45 // Fog
        800 -> 0 // Clear
        801 -> 1 // Partly Cloudy
        802 -> 2 // Cloudy
        803, 804 -> 3 // Overcast
        else -> 0
    }

    private fun wapiToWmoCode(wapiCode: Int): Int = when (wapiCode) {
        1000 -> 0 // Clear
        1003 -> 1 // Partly Cloudy
        1006 -> 2 // Cloudy
        1009 -> 3 // Overcast
        1030, 1135, 1147 -> 45 // Fog
        1063, 1180, 1183, 1186, 1189, 1192, 1195 -> 61 // Rain
        1066, 1210, 1213, 1216, 1219, 1222, 1225 -> 71 // Snow
        1087, 1273, 1276, 1279, 1282 -> 95 // Thunderstorm
        else -> 0
    }

    private fun vcIconToWmoCode(icon: String): Int = when (icon) {
        "clear-day", "clear-night" -> 0
        "partly-cloudy-day", "partly-cloudy-night" -> 1
        "cloudy" -> 2
        "fog" -> 45
        "wind", "rain" -> 61
        "snow" -> 71
        "thunder-rain", "thunder-showers-day", "thunder-showers-night" -> 95
        else -> 0
    }

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing Drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing Rain"
        71, 73, 75 -> "Snow fall"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }
}
