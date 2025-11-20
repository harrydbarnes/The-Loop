package com.example.theloop.utils;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.example.theloop.R;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class AppUtils {

    private static final String TAG = "AppUtils";

    private AppUtils() {
        // This class is not meant to be instantiated.
    }

    public static String formatPublishedAt(@NonNull Context context, String publishedAt) {
        return formatPublishedAt(context, publishedAt, Clock.systemDefaultZone());
    }

    public static String formatPublishedAt(@NonNull Context context, String publishedAt, Clock clock) {
        try {
            ZonedDateTime now = ZonedDateTime.now(clock);
            ZonedDateTime zdt = ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);

            Duration duration = Duration.between(zdt, now);

            long minutes = duration.toMinutes();
            if (minutes < 1) return context.getString(R.string.just_now);
            if (minutes < 60) return context.getResources().getQuantityString(R.plurals.time_minutes_ago, (int) minutes, (int) minutes);

            long hours = duration.toHours();
            if (hours < 24) return context.getResources().getQuantityString(R.plurals.time_hours_ago, (int) hours, (int) hours);

            long days = duration.toDays();
            return context.getResources().getQuantityString(R.plurals.time_days_ago, (int) days, (int) days);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Could not parse date: " + publishedAt, e);
            return context.getString(R.string.just_now);
        }
    }

    @StringRes
    public static int getWeatherDescription(int weatherCode) {
        switch (weatherCode) {
            case 0: return R.string.weather_clear_sky;
            case 1: return R.string.weather_mainly_clear;
            case 2: return R.string.weather_partly_cloudy;
            case 3: return R.string.weather_overcast;
            case 45: case 48: return R.string.weather_fog;
            case 51: case 53: case 55: return R.string.weather_drizzle;
            case 61: case 63: case 65: return R.string.weather_rain;
            case 71: case 73: case 75: return R.string.weather_snow_fall;
            case 80: case 81: case 82: return R.string.weather_rain_showers;
            case 95: return R.string.weather_thunderstorm;
            default: return R.string.weather_unknown;
        }
    }

    @StringRes
    public static int getDailyForecast(int weatherCode) {
        switch (weatherCode) {
            case 0: return R.string.forecast_clear;
            case 1: return R.string.forecast_mainly_clear;
            case 2: return R.string.forecast_partly_cloudy;
            case 3: return R.string.forecast_overcast;
            case 45: case 48: return R.string.forecast_fog;
            case 51: case 53: case 55: return R.string.forecast_drizzle;
            case 61: case 63: case 65: return R.string.forecast_rain;
            case 71: case 73: case 75: return R.string.forecast_snow;
            case 80: case 81: case 82: return R.string.forecast_showers;
            case 95: return R.string.forecast_thunderstorm;
            default: return R.string.forecast_unavailable;
        }
    }

    @DrawableRes
    public static int getWeatherIconResource(int weatherCode) {
        switch (weatherCode) {
            case 0: return R.drawable.ic_weather_sunny;
            case 1: case 2: return R.drawable.ic_weather_partly_cloudy;
            case 3: return R.drawable.ic_weather_cloudy;
            case 45: case 48: return R.drawable.ic_weather_foggy;
            case 51: case 53: case 55: case 61: case 63: case 65: case 80: case 81: case 82:
                return R.drawable.ic_weather_rainy;
            case 71: case 73: case 75: case 85: case 86:
                return R.drawable.ic_weather_snowy;
            case 95: case 96: case 99:
                return R.drawable.ic_weather_thunderstorm;
            default:
                return R.drawable.ic_weather_cloudy;
        }
    }

    public static String formatEventTime(@NonNull Context context, long startTime, long endTime) {
        return DateUtils.formatDateRange(context, startTime, endTime, DateUtils.FORMAT_SHOW_TIME);
    }
}
