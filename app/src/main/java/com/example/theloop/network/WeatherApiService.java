package com.example.theloop.network;

import com.example.theloop.models.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("v1/forecast")
    Call<WeatherResponse> getWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String current,
            @Query("daily") String daily,
            @Query("temperature_unit") String tempUnit,
            @Query("timezone") String timezone
    );
}
