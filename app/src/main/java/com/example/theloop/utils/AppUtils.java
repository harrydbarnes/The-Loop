package com.example.theloop.utils;

import android.content.Context;
import android.text.format.DateUtils;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.example.theloop.R;

public final class AppUtils {

    private static final String TAG = "AppUtils";

    private static final java.time.format.DateTimeFormatter WEATHER_DATE_INPUT_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.getDefault());
    private static final java.time.format.DateTimeFormatter WEATHER_DATE_DAY_FORMAT = java.time.format.DateTimeFormatter.ofPattern("EEE d", java.util.Locale.getDefault());

    private AppUtils() {
        // This class is not meant to be instantiated.
    }

    /**
     * Pre-formats forecast dates to avoid repeated parsing during RecyclerView binding.
     *
     * @param rawDates List of date strings in "yyyy-MM-dd" format.
     * @return List of formatted date strings (e.g., "Mon 1"), or "-" on error.
     */
    public static java.util.List<String> formatForecastDates(java.util.List<String> rawDates) {
        if (rawDates == null) {
            return java.util.Collections.emptyList();
        }

        java.util.List<String> formatted = new java.util.ArrayList<>(rawDates.size());
        for (String raw : rawDates) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(raw, WEATHER_DATE_INPUT_FORMAT);
                formatted.add(date.format(WEATHER_DATE_DAY_FORMAT));
            } catch (java.time.format.DateTimeParseException e) {
                formatted.add("-");
            }
        }
        return formatted;
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
            case 96, 99 -> R.string.weather_thunderstorm_with_hail;
            default -> R.string.weather_unknown;
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
