package com.omar.weatherapp.data.api

import com.omar.weatherapp.data.models.WapiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WapiService {
    @GET("forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") query: String, // "lat,lon"
        @Query("days") days: Int = 7,
        @Query("aqi") aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): WapiResponse

    companion object {
        const val BASE_URL = "https://api.weatherapi.com/v1/"
    }
}
