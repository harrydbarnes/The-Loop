package com.example.theloop.models

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("current") val current: CurrentWeather,
    @SerializedName("daily") val daily: DailyWeather?
)
