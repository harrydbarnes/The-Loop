package com.example.theloop;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
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

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void formatPublishedAt_handlesRecentTime() {
        String now = ZonedDateTime.now().toString();
        String result = AppUtils.formatPublishedAt(context, now);
        assertEquals("Just now", result);
    }

    @Test
    public void formatPublishedAt_handlesMinutesAgo() {
        String minutesAgo = ZonedDateTime.now().minusMinutes(5).toString();
        String result = AppUtils.formatPublishedAt(context, minutesAgo);
        // Note: The exact string depends on the plural resource configuration.
        // In English, "5 minutes ago".
        assertEquals("5 minutes ago", result);
    }

    @Test
    public void formatPublishedAt_handlesHoursAgo() {
        String hoursAgo = ZonedDateTime.now().minusHours(3).toString();
        String result = AppUtils.formatPublishedAt(context, hoursAgo);
        assertEquals("3 hours ago", result);
    }

    @Test
    public void formatPublishedAt_handlesDaysAgo() {
        String daysAgo = ZonedDateTime.now().minusDays(2).toString();
        String result = AppUtils.formatPublishedAt(context, daysAgo);
        assertEquals("2 days ago", result);
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
