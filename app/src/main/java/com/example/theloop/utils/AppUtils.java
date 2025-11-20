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
        return switch (weatherCode) {
            case 0 -> R.string.weather_clear_sky;
            case 1 -> R.string.weather_mainly_clear;
            case 2 -> R.string.weather_partly_cloudy;
            case 3 -> R.string.weather_overcast;
            case 45, 48 -> R.string.weather_fog;
            case 51, 53, 55 -> R.string.weather_drizzle;
            case 61, 63, 65 -> R.string.weather_rain;
            case 71, 73, 75 -> R.string.weather_snow_fall;
            case 80, 81, 82 -> R.string.weather_rain_showers;
            case 95 -> R.string.weather_thunderstorm;
            default -> R.string.weather_unknown;
        };
    }

    @StringRes
    public static int getDailyForecast(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> R.string.forecast_clear;
            case 1 -> R.string.forecast_mainly_clear;
            case 2 -> R.string.forecast_partly_cloudy;
            case 3 -> R.string.forecast_overcast;
            case 45, 48 -> R.string.forecast_fog;
            case 51, 53, 55 -> R.string.forecast_drizzle;
            case 61, 63, 65 -> R.string.forecast_rain;
            case 71, 73, 75 -> R.string.forecast_snow;
            case 80, 81, 82 -> R.string.forecast_showers;
            case 95 -> R.string.forecast_thunderstorm;
            default -> R.string.forecast_unavailable;
        };
    }

    @DrawableRes
    public static int getWeatherIconResource(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> R.drawable.ic_weather_sunny;
            case 1, 2 -> R.drawable.ic_weather_partly_cloudy;
            case 3 -> R.drawable.ic_weather_cloudy;
            case 45, 48 -> R.drawable.ic_weather_foggy;
            case 51, 53, 55, 61, 63, 65, 80, 81, 82 -> R.drawable.ic_weather_rainy;
            case 71, 73, 75, 85, 86 -> R.drawable.ic_weather_snowy;
            case 95, 96, 99 -> R.drawable.ic_weather_thunderstorm;
            default -> R.drawable.ic_weather_cloudy;
        };
    }

    public static String formatEventTime(@NonNull Context context, long startTime, long endTime) {
        return DateUtils.formatDateRange(context, startTime, endTime, DateUtils.FORMAT_SHOW_TIME);
    }
}
