package com.example.theloop.network

import com.example.theloop.models.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("daily") daily: String,
        @Query("temperature_unit") tempUnit: String,
        @Query("timezone") timezone: String
    ): Response<WeatherResponse>
}
