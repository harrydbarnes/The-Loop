package com.example.theloop.models;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("current")
    private CurrentWeather current;

    @SerializedName("daily")
    private DailyWeather daily;

    // Getters
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public CurrentWeather getCurrent() {
        return current;
    }

    public DailyWeather getDaily() {
        return daily;
    }
}
