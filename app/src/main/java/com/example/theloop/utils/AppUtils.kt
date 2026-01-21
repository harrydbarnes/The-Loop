package com.example.theloop.utils

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.theloop.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object AppUtils {
    private val WEATHER_DATE_INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val WEATHER_DATE_DAY_FORMAT = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())

    fun formatForecastDates(rawDates: List<String>?): List<String> {
        if (rawDates == null) {
            return emptyList()
        }
        return rawDates.map { raw ->
            try {
                val date = LocalDate.parse(raw, WEATHER_DATE_INPUT_FORMAT)
                date.format(WEATHER_DATE_DAY_FORMAT)
            } catch (e: DateTimeParseException) {
                "-"
            }
        }
    }

    @JvmStatic
    @StringRes
    fun getWeatherDescription(weatherCode: Int): Int {
        return when (weatherCode) {
            0 -> R.string.weather_clear_sky
            1 -> R.string.weather_mainly_clear
            2 -> R.string.weather_partly_cloudy
            3 -> R.string.weather_overcast
            45, 48 -> R.string.weather_fog
            51, 53, 55 -> R.string.weather_drizzle
            61, 63, 65 -> R.string.weather_rain
            71, 73, 75 -> R.string.weather_snow_fall
            80, 81, 82 -> R.string.weather_rain_showers
            95 -> R.string.weather_thunderstorm
            96, 99 -> R.string.weather_thunderstorm_with_hail
            else -> R.string.weather_unknown
        }
    }

    @JvmStatic
    @DrawableRes
    fun getWeatherIconResource(weatherCode: Int): Int {
        return when (weatherCode) {
            0 -> R.drawable.ic_weather_sunny
            1, 2 -> R.drawable.ic_weather_partly_cloudy
            3 -> R.drawable.ic_weather_cloudy
            45, 48 -> R.drawable.ic_weather_foggy
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> R.drawable.ic_weather_rainy
            71, 73, 75, 85, 86 -> R.drawable.ic_weather_snowy
            95, 96, 99 -> R.drawable.ic_weather_thunderstorm
            else -> R.drawable.ic_weather_cloudy
        }
    }

    fun formatEventTime(context: Context, startTime: Long, endTime: Long): String {
        return DateUtils.formatDateRange(context, startTime, endTime, DateUtils.FORMAT_SHOW_TIME)
    }
}
