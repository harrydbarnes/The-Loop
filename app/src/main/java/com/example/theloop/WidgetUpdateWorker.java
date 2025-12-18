package com.example.theloop;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.theloop.models.WeatherResponse;
import com.example.theloop.network.RetrofitClient;
import com.example.theloop.network.WeatherApiService;
import com.example.theloop.utils.AppConstants;
import com.google.gson.Gson;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class WidgetUpdateWorker extends Worker {

    private static final String TAG = "WidgetUpdateWorker";

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Fetching weather for widget update...");

        android.content.SharedPreferences prefs = getApplicationContext().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        double lat;
        double lon;

        String latStr = prefs.getString(AppConstants.KEY_LATITUDE, null);
        String lonStr = prefs.getString(AppConstants.KEY_LONGITUDE, null);

        if (latStr == null || lonStr == null) {
            Log.w(TAG, "No location available for widget update. Skipping weather fetch.");
            return Result.success();
        }

        try {
            lat = Double.parseDouble(latStr);
            lon = Double.parseDouble(lonStr);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Could not parse lat/lon from SharedPreferences", e);
            return Result.failure();
        }

        // Fetch Weather
        try {
            String unit = prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT);

            WeatherApiService apiService = RetrofitClient.getClient().create(WeatherApiService.class);
            Call<WeatherResponse> call = apiService.getWeather(lat, lon,
                    "temperature_2m,weather_code", "weather_code,temperature_2m_max,temperature_2m_min", unit, "auto");

            Response<WeatherResponse> response = call.execute(); // Synchronous
            if (response.isSuccessful() && response.body() != null) {
                // Save to cache
                String json = new Gson().toJson(response.body());
                getApplicationContext().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putString(AppConstants.WEATHER_CACHE_KEY, json).apply();

                // Trigger widget update explicitly
                android.content.Intent intent = new android.content.Intent(getApplicationContext(), DayAheadWidget.class);
                intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = android.appwidget.AppWidgetManager.getInstance(getApplicationContext())
                        .getAppWidgetIds(new android.content.ComponentName(getApplicationContext(), DayAheadWidget.class));
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                getApplicationContext().sendBroadcast(intent);

                return Result.success();
            }
        } catch (IOException e) {
            Log.e(TAG, "Widget update failed", e);
            return Result.retry();
        }

        return Result.failure();
    }
}
