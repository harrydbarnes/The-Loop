package com.example.theloop.utils;

import android.content.Context;
import android.text.TextUtils;

import com.example.theloop.R;
import com.example.theloop.models.Article;
import com.example.theloop.models.CalendarEvent;
import com.example.theloop.models.WeatherResponse;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SummaryUtils {

    public static String generateSummary(Context context, WeatherResponse weather, List<CalendarEvent> events, int totalEvents, Article topHeadline, String userName, boolean calendarError) {
        if (weather == null) return null;

        String condition = context.getString(AppUtils.getWeatherDescription(weather.getCurrent().getWeatherCode()));
        double temp = weather.getCurrent().getTemperature();

        String nextEventTitle = (events != null && !events.isEmpty()) ? events.get(0).getTitle() : "";
        String newsTitle = (topHeadline != null) ? topHeadline.getTitle() : "No major news";

        String timeGreeting = getTimeBasedGreeting();

        String eventsSummary;
        if (calendarError) {
            eventsSummary = context.getString(R.string.calendar_error);
        } else if (totalEvents > 0) {
            eventsSummary = context.getResources().getQuantityString(R.plurals.daily_summary_events, totalEvents, totalEvents, nextEventTitle);
        } else {
            eventsSummary = context.getResources().getQuantityString(R.plurals.daily_summary_events, 0);
        }

        return String.format(Locale.getDefault(), context.getString(R.string.daily_summary_format),
                timeGreeting, userName, condition, temp, eventsSummary, newsTitle
        );
    }

    private static String getTimeBasedGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        if (timeOfDay >= 0 && timeOfDay < 12) return "Good morning";
        else if (timeOfDay >= 12 && timeOfDay < 17) return "Good afternoon";
        else return "Good evening";
    }
}
