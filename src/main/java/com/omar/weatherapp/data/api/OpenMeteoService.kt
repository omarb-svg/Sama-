package com.omar.weatherapp.data.api

import com.omar.weatherapp.data.models.GeocodingResponse
import com.omar.weatherapp.data.models.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_PARAMS,
        @Query("hourly") hourly: String = HOURLY_PARAMS,
        @Query("daily") daily: String = DAILY_PARAMS,
        @Query("temperature_unit") temperatureUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windSpeedUnit: String = "mph",
        @Query("precipitation_unit") precipitationUnit: String = "inch",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        const val CURRENT_PARAMS = "temperature_2m," +
                "apparent_temperature," +
                "relative_humidity_2m," +
                "precipitation," +
                "weather_code," +
                "wind_speed_10m," +
                "wind_direction_10m," +
                "surface_pressure," +
                "visibility," +
                "uv_index," +
                "is_day," +
                "cloud_cover"

        const val HOURLY_PARAMS = "temperature_2m," +
                "apparent_temperature," +
                "weather_code," +
                "precipitation_probability," +
                "wind_speed_10m," +
                "wind_direction_10m," +
                "cloud_cover," +
                "dew_point_2m," +
                "surface_pressure," +
                "visibility," +
                "uv_index"

        const val DAILY_PARAMS = "weather_code," +
                "temperature_2m_max," +
                "temperature_2m_min," +
                "apparent_temperature_max," +
                "apparent_temperature_min," +
                "precipitation_probability_max," +
                "uv_index_max," +
                "sunrise," +
                "sunset"
    }
}

interface GeocodingService {

    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"
    }
}
