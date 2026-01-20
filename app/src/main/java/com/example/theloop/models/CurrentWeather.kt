package com.example.theloop.models

import com.google.gson.annotations.SerializedName

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("time") val time: String
)
