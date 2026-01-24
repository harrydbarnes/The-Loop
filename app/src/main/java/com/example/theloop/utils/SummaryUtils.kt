package com.example.theloop.utils

import android.content.Context
import com.example.theloop.R
import com.example.theloop.models.Article
import com.example.theloop.models.CalendarEvent
import com.example.theloop.models.WeatherResponse
import java.util.Calendar
import java.util.Locale

object SummaryUtils {

    @JvmStatic
    fun generateSummary(
        context: Context,
        weather: WeatherResponse?,
        events: List<CalendarEvent>?,
        totalEvents: Int,
        topHeadline: Article?,
        userName: String,
        calendarError: Boolean
    ): String? {
        if (weather == null) return null

        val condition = context.getString(AppUtils.getWeatherDescription(weather.current.weatherCode))
        val temp = weather.current.temperature

        val nextEventTitle = if (!events.isNullOrEmpty()) events[0].title else ""
        val newsTitle = topHeadline?.title ?: "No major news"

        val timeGreeting = getTimeBasedGreeting()

        val eventsSummary = if (calendarError) {
            context.getString(R.string.calendar_error)
        } else if (totalEvents > 0) {
            context.resources.getQuantityString(R.plurals.daily_summary_events, totalEvents, totalEvents, nextEventTitle)
        } else {
            context.resources.getQuantityString(R.plurals.daily_summary_events, 0)
        }

        return String.format(
            Locale.getDefault(),
            context.getString(R.string.daily_summary_format),
            timeGreeting, userName, condition, temp, eventsSummary, newsTitle
        )
    }

    @JvmStatic
    fun getTimeBasedGreeting(): String {
        val c = Calendar.getInstance()
        val timeOfDay = c.get(Calendar.HOUR_OF_DAY)
        return when (timeOfDay) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
