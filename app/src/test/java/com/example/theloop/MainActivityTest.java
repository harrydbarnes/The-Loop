package com.example.theloop;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

import com.example.theloop.utils.AppUtils;

import java.time.ZonedDateTime;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class MainActivityTest {

    // Note: We are now testing AppUtils directly as logic has been moved there.
    // MainActivity logic is now primarily UI binding which is harder to unit test without instrumented tests.

    @Test
    public void formatPublishedAt_handlesRecentTime() {
        String now = ZonedDateTime.now().toString();
        String result = AppUtils.formatPublishedAt(now);
        assertEquals("Just now", result);
    }

    @Test
    public void formatPublishedAt_handlesMinutesAgo() {
        String minutesAgo = ZonedDateTime.now().minusMinutes(5).toString();
        String result = AppUtils.formatPublishedAt(minutesAgo);
        assertEquals("5m ago", result);
    }

    @Test
    public void formatPublishedAt_handlesHoursAgo() {
        String hoursAgo = ZonedDateTime.now().minusHours(3).toString();
        String result = AppUtils.formatPublishedAt(hoursAgo);
        assertEquals("3h ago", result);
    }

    @Test
    public void formatPublishedAt_handlesDaysAgo() {
        String daysAgo = ZonedDateTime.now().minusDays(2).toString();
        String result = AppUtils.formatPublishedAt(daysAgo);
        assertEquals("2d ago", result);
    }

    @Test
    public void getWeatherDescription_mapsCodesCorrectly() {
        assertEquals("Clear sky", AppUtils.getWeatherDescription(0));
        assertEquals("Partly cloudy", AppUtils.getWeatherDescription(2));
        assertEquals("Fog", AppUtils.getWeatherDescription(45));
        assertEquals("Rain", AppUtils.getWeatherDescription(61));
        assertEquals("Snow fall", AppUtils.getWeatherDescription(75));
        assertEquals("Thunderstorm", AppUtils.getWeatherDescription(95));
        assertEquals("Unknown", AppUtils.getWeatherDescription(1000));
    }

    @Test
    public void getDailyForecast_mapsCodesCorrectly() {
        assertEquals("Expect clear skies today.", AppUtils.getDailyForecast(0));
        assertEquals("Partly cloudy today.", AppUtils.getDailyForecast(2));
        assertEquals("Fog is expected today.", AppUtils.getDailyForecast(48));
        assertEquals("Rain expected today.", AppUtils.getDailyForecast(63));
        assertEquals("Snowfall is expected.", AppUtils.getDailyForecast(73));
        assertEquals("Thunderstorms possible.", AppUtils.getDailyForecast(95));
        assertEquals("Weather data unavailable.", AppUtils.getDailyForecast(500));
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
