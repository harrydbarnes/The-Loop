package com.example.theloop.utils;

import android.util.Log;
import com.example.theloop.R;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class AppUtils {

    private static final String TAG = "AppUtils";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());

    private AppUtils() {
        // This class is not meant to be instantiated.
    }

    public static String formatPublishedAt(String publishedAt) {
        try {
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime zdt = ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);

            long minutes = ChronoUnit.MINUTES.between(zdt, now);
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + "m ago";

            long hours = ChronoUnit.HOURS.between(zdt, now);
            if (hours < 24) return hours + "h ago";

            long days = ChronoUnit.DAYS.between(zdt, now);
            return days + "d ago";
        } catch (Exception e) {
            Log.e(TAG, "Could not parse date: " + publishedAt, e);
            return "Just now";
        }
    }

    public static String getWeatherDescription(int weatherCode) {
        switch (weatherCode) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return "Drizzle";
            case 61: case 63: case 65: return "Rain";
            case 71: case 73: case 75: return "Snow fall";
            case 80: case 81: case 82: return "Rain showers";
            case 95: return "Thunderstorm";
            default: return "Unknown";
        }
    }

    public static String getDailyForecast(int weatherCode) {
        switch (weatherCode) {
            case 0: return "Expect clear skies today.";
            case 1: return "Mainly clear skies expected.";
            case 2: return "Partly cloudy today.";
            case 3: return "Expect overcast skies.";
            case 45: case 48: return "Fog is expected today.";
            case 51: case 53: case 55: return "Light drizzle possible.";
            case 61: case 63: case 65: return "Rain expected today.";
            case 71: case 73: case 75: return "Snowfall is expected.";
            case 80: case 81: case 82: return "Expect rain showers.";
            case 95: return "Thunderstorms possible.";
            default: return "Weather data unavailable.";
        }
    }

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

    public static String formatEventTime(long startTime, long endTime) {
        String start = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
        String end = Instant.ofEpochMilli(endTime).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
        return start + " - " + end;
    }
}
