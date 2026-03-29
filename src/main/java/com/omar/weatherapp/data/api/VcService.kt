package com.omar.weatherapp.data.api

import com.omar.weatherapp.data.models.VcResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VcService {
    @GET("{location}")
    suspend fun getTimeline(
        @Path("location") location: String, // "lat,lon"
        @Query("key") apiKey: String,
        @Query("unitGroup") unitGroup: String = "us",
        @Query("include") include: String = "days,hours,current",
        @Query("contentType") contentType: String = "json"
    ): VcResponse

    companion object {
        const val BASE_URL = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
    }
}
