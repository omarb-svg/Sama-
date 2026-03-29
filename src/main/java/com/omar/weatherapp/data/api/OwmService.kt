package com.omar.weatherapp.data.api

import com.omar.weatherapp.data.models.OwmResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OwmService {
    @GET("onecall")
    suspend fun getOneCall(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "imperial",
        @Query("exclude") exclude: String = "minutely,alerts"
    ): OwmResponse

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    }
}
