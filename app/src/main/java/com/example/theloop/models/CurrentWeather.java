package com.example.theloop.models;

import com.google.gson.annotations.SerializedName;

public class CurrentWeather {

    @SerializedName("time")
    private String time;

    @SerializedName("temperature_2m")
    private double temperature;

    @SerializedName("weather_code")
    private int weatherCode;

    // Getters
    public String getTime() {
        return time;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getWeatherCode() {
        return weatherCode;
    }
}
