package com.example.theloop.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DailyWeather {

    @SerializedName("time")
    private List<String> time;

    @SerializedName("weather_code")
    private List<Integer> weatherCode;

    @SerializedName("temperature_2m_max")
    private List<Double> temperatureMax;

    @SerializedName("temperature_2m_min")
    private List<Double> temperatureMin;

    // Getters
    public List<String> getTime() {
        return time;
    }

    public List<Integer> getWeatherCode() {
        return weatherCode;
    }

    public List<Double> getTemperatureMax() {
        return temperatureMax;
    }

    public List<Double> getTemperatureMin() {
        return temperatureMin;
    }
}
