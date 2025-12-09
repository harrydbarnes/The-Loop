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
import com.google.gson.Gson;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class WidgetUpdateWorker extends Worker {

    private static final String TAG = "WidgetUpdateWorker";
    private static final String PREFS_NAME = "TheLoopPrefs";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String WEATHER_CACHE_KEY = "weather_cache";
    private static final double DEFAULT_LATITUDE = 51.5480;
    private static final double DEFAULT_LONGITUDE = -0.1030;

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Fetching weather for widget update...");

        android.content.SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        double lat = DEFAULT_LATITUDE;
        double lon = DEFAULT_LONGITUDE;

        String latStr = prefs.getString("last_latitude", null);
        String lonStr = prefs.getString("last_longitude", null);

        if (latStr != null && lonStr != null) {
            try {
                lat = Double.parseDouble(latStr);
                lon = Double.parseDouble(lonStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Could not parse lat/lon from SharedPreferences", e);
            }
        }

        // Fetch Weather
        try {
            String unit = prefs.getString(KEY_TEMP_UNIT, "celsius");

            WeatherApiService apiService = RetrofitClient.getClient().create(WeatherApiService.class);
            Call<WeatherResponse> call = apiService.getWeather(lat, lon,
                    "temperature_2m,weather_code", "weather_code,temperature_2m_max,temperature_2m_min", unit, "auto");

            Response<WeatherResponse> response = call.execute(); // Synchronous
            if (response.isSuccessful() && response.body() != null) {
                // Save to cache
                String json = new Gson().toJson(response.body());
                getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putString(WEATHER_CACHE_KEY, json).apply();

                // Trigger widget update
                // The widget reads from SharedPreferences, so we just need to notify it.
                // But we can't easily send broadcast from here without Context? We have context.
                // DayAheadWidget.updateAppWidget...
                // Actually sending the intent is the standard way.

                // We'll trust DayAheadWidget to read the cache when it updates.
                // But we should force an update.
                // However, standard widgets update on interval.
                // We can request update.

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
