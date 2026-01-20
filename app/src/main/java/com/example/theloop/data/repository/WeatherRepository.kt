package com.example.theloop.data.repository

import com.example.theloop.data.local.dao.WeatherDao
import com.example.theloop.data.local.entity.WeatherEntity
import com.example.theloop.models.WeatherResponse
import com.example.theloop.network.WeatherApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val api: WeatherApiService,
    private val dao: WeatherDao,
    private val gson: Gson
) {
    val weatherData: Flow<WeatherResponse?> = dao.getWeatherFlow().map { entity ->
        if (entity != null) {
            try {
                gson.fromJson(entity.json, WeatherResponse::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun refresh(lat: Double, lon: Double, unit: String) {
        withContext(Dispatchers.IO) {
            try {
                // Ensure timezone is valid, "auto" works for Open-Meteo
                val response = api.getWeather(
                    lat,
                    lon,
                    "temperature_2m,weather_code",
                    "weather_code,temperature_2m_max,temperature_2m_min",
                    unit,
                    "auto"
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val json = gson.toJson(body)
                        dao.insertWeather(WeatherEntity(id = 0, json = json, lastUpdated = System.currentTimeMillis()))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
