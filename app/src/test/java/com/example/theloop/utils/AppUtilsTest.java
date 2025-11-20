package com.example.theloop.utils;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

import com.example.theloop.R;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class AppUtilsTest {

    private Context context = ApplicationProvider.getApplicationContext();
    private Clock fixedClock = Clock.fixed(Instant.parse("2023-10-05T12:00:00Z"), ZoneId.of("UTC"));

    @Test
    public void formatPublishedAt_handlesRecentTime() {
        String now = ZonedDateTime.now(fixedClock).format(DateTimeFormatter.ISO_DATE_TIME);
        String result = AppUtils.formatPublishedAt(context, now, fixedClock);
        assertEquals(context.getString(R.string.just_now), result);
    }

    @Test
    public void formatPublishedAt_handlesOneMinuteAgo() {
        String oneMinuteAgo = ZonedDateTime.now(fixedClock).minusMinutes(1).format(DateTimeFormatter.ISO_DATE_TIME);
        String result = AppUtils.formatPublishedAt(context, oneMinuteAgo, fixedClock);
        assertEquals(context.getResources().getQuantityString(R.plurals.time_minutes_ago, 1, 1), result);
    }

    @Test
    public void formatPublishedAt_handlesMinutesAgo() {
        String minutesAgo = ZonedDateTime.now(fixedClock).minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME);
        String result = AppUtils.formatPublishedAt(context, minutesAgo, fixedClock);
        assertEquals(context.getResources().getQuantityString(R.plurals.time_minutes_ago, 5, 5), result);
    }

    @Test
    public void formatPublishedAt_handlesOneHourAgo() {
        String oneHourAgo = ZonedDateTime.now(fixedClock).minusHours(1).format(DateTimeFormatter.ISO_DATE_TIME);
        String result = AppUtils.formatPublishedAt(context, oneHourAgo, fixedClock);
        assertEquals(context.getResources().getQuantityString(R.plurals.time_hours_ago, 1, 1), result);
    }

    @Test
    public void formatPublishedAt_handlesHoursAgo() {
        String hoursAgo = ZonedDateTime.now(fixedClock).minusHours(3).format(DateTimeFormatter.ISO_DATE_TIME);
        String result = AppUtils.formatPublishedAt(context, hoursAgo, fixedClock);
        assertEquals(context.getResources().getQuantityString(R.plurals.time_hours_ago, 3, 3), result);
    }

    @Test
    public void formatPublishedAt_handlesOneDayAgo() {
        String oneDayAgo = ZonedDateTime.now(fixedClock).minusDays(1).format(DateTimeFormatter.ISO_DATE_TIME);
        String result = AppUtils.formatPublishedAt(context, oneDayAgo, fixedClock);
        assertEquals(context.getResources().getQuantityString(R.plurals.time_days_ago, 1, 1), result);
    }

    @Test
    public void formatPublishedAt_handlesDaysAgo() {
        String daysAgo = ZonedDateTime.now(fixedClock).minusDays(2).format(DateTimeFormatter.ISO_DATE_TIME);
        String result = AppUtils.formatPublishedAt(context, daysAgo, fixedClock);
        assertEquals(context.getResources().getQuantityString(R.plurals.time_days_ago, 2, 2), result);
    }

    @Test
    public void getWeatherDescription_mapsCodesCorrectly() {
        assertEquals(R.string.weather_clear_sky, AppUtils.getWeatherDescription(0));
        assertEquals(R.string.weather_partly_cloudy, AppUtils.getWeatherDescription(2));
        assertEquals(R.string.weather_fog, AppUtils.getWeatherDescription(45));
        assertEquals(R.string.weather_rain, AppUtils.getWeatherDescription(61));
        assertEquals(R.string.weather_snow_fall, AppUtils.getWeatherDescription(75));
        assertEquals(R.string.weather_thunderstorm, AppUtils.getWeatherDescription(95));
        assertEquals(R.string.weather_unknown, AppUtils.getWeatherDescription(1000));
    }

    @Test
    public void getDailyForecast_mapsCodesCorrectly() {
        assertEquals(R.string.forecast_clear, AppUtils.getDailyForecast(0));
        assertEquals(R.string.forecast_partly_cloudy, AppUtils.getDailyForecast(2));
        assertEquals(R.string.forecast_fog, AppUtils.getDailyForecast(48));
        assertEquals(R.string.forecast_rain, AppUtils.getDailyForecast(63));
        assertEquals(R.string.forecast_snow, AppUtils.getDailyForecast(73));
        assertEquals(R.string.forecast_thunderstorm, AppUtils.getDailyForecast(95));
        assertEquals(R.string.forecast_unavailable, AppUtils.getDailyForecast(500));
    }

    @Test
    public void getWeatherIconResource_mapsCodesToCorrectDrawables() {
        assertEquals(R.drawable.ic_weather_sunny, AppUtils.getWeatherIconResource(0));
        assertEquals(R.drawable.ic_weather_partly_cloudy, AppUtils.getWeatherIconResource(2));
        assertEquals(R.drawable.ic_weather_cloudy, AppUtils.getWeatherIconResource(3));
        assertEquals(R.drawable.ic_weather_foggy, AppUtils.getWeatherIconResource(45));
        assertEquals(R.drawable.ic_weather_rainy, AppUtils.getWeatherIconResource(61));
        assertEquals(R.drawable.ic_weather_snowy, AppUtils.getWeatherIconResource(71));
        assertEquals(R.drawable.ic_weather_thunderstorm, AppUtils.getWeatherIconResource(95));
        assertEquals(R.drawable.ic_weather_cloudy, AppUtils.getWeatherIconResource(999)); // Default case
    }
}
