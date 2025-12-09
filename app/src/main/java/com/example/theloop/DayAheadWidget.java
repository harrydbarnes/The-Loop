package com.example.theloop;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.theloop.models.WeatherResponse;
import com.example.theloop.utils.AppUtils;
import com.google.gson.Gson;

import java.util.Locale;

public class DayAheadWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "TheLoopPrefs";
    private static final String WEATHER_CACHE_KEY = "weather_cache";
    private static final String KEY_SUMMARY_CACHE = "summary_cache"; // New cache for summary text

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String summary = prefs.getString(KEY_SUMMARY_CACHE, "Open The Loop to see your day ahead.");
        String weatherJson = prefs.getString(WEATHER_CACHE_KEY, null);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_day_ahead);
        views.setTextViewText(R.id.widget_summary, summary);

        if (weatherJson != null) {
            try {
                WeatherResponse weather = new Gson().fromJson(weatherJson, WeatherResponse.class);
                if (weather != null && weather.getCurrent() != null) {
                    views.setTextViewText(R.id.widget_temp, String.format(Locale.getDefault(), "%.0f°", weather.getCurrent().getTemperature()));
                    views.setImageViewResource(R.id.widget_weather_icon, AppUtils.getWeatherIconResource(weather.getCurrent().getWeatherCode()));
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
