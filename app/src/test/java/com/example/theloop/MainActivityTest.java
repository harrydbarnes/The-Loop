package com.example.theloop;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import android.content.SharedPreferences;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class MainActivityTest {

    @Mock
    SharedPreferences mockPrefs;

    @Mock
    SharedPreferences.Editor mockEditor;

    @Spy
    private MainActivity mainActivity = Robolectric.buildActivity(MainActivity.class).create().get();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mockPrefs).when(mainActivity).getSharedPreferences(anyString(), anyInt());
        when(mockPrefs.edit()).thenReturn(mockEditor);
    }

    @Test
    public void formatPublishedAt_handlesRecentTime() {
        String now = ZonedDateTime.now().toString();
        String result = mainActivity.formatPublishedAt(now);
        assertTrue(result.contains("m ago"));
    }

    @Test
    public void formatPublishedAt_handlesHoursAgo() {
        String hoursAgo = ZonedDateTime.now().minusHours(3).toString();
        String result = mainActivity.formatPublishedAt(hoursAgo);
        assertEquals("3h ago", result);
    }

    @Test
    public void formatPublishedAt_handlesDaysAgo() {
        String daysAgo = ZonedDateTime.now().minusDays(2).toString();
        String result = mainActivity.formatPublishedAt(daysAgo);
        assertEquals("2d ago", result);
    }

    @Test
    public void getWeatherDescription_mapsCodesCorrectly() {
        assertEquals("Clear sky", mainActivity.getWeatherDescription(0));
        assertEquals("Partly cloudy", mainActivity.getWeatherDescription(2));
        assertEquals("Fog", mainActivity.getWeatherDescription(45));
        assertEquals("Rain", mainActivity.getWeatherDescription(61));
        assertEquals("Snow fall", mainActivity.getWeatherDescription(75));
        assertEquals("Thunderstorm", mainActivity.getWeatherDescription(95));
        assertEquals("Unknown", mainActivity.getWeatherDescription(1000));
    }

    @Test
    public void getDailyForecast_mapsCodesCorrectly() {
        assertEquals("Expect clear skies today.", mainActivity.getDailyForecast(0));
        assertEquals("Partly cloudy today.", mainActivity.getDailyForecast(2));
        assertEquals("Fog is expected today.", mainActivity.getDailyForecast(48));
        assertEquals("Rain expected today.", mainActivity.getDailyForecast(63));
        assertEquals("Snowfall is expected.", mainActivity.getDailyForecast(73));
        assertEquals("Thunderstorms possible.", mainActivity.getDailyForecast(95));
        assertEquals("Weather data unavailable.", mainActivity.getDailyForecast(500));
    }

    @Test
    public void getWeatherIconResource_mapsCodesToCorrectDrawables() {
        assertEquals(R.drawable.ic_weather_sunny, mainActivity.getWeatherIconResource(0));
        assertEquals(R.drawable.ic_weather_partly_cloudy, mainActivity.getWeatherIconResource(2));
        assertEquals(R.drawable.ic_weather_cloudy, mainActivity.getWeatherIconResource(3));
        assertEquals(R.drawable.ic_weather_foggy, mainActivity.getWeatherIconResource(45));
        assertEquals(R.drawable.ic_weather_rainy, mainActivity.getWeatherIconResource(61));
        assertEquals(R.drawable.ic_weather_snowy, mainActivity.getWeatherIconResource(71));
        assertEquals(R.drawable.ic_weather_thunderstorm, mainActivity.getWeatherIconResource(95));
        assertEquals(R.drawable.ic_weather_cloudy, mainActivity.getWeatherIconResource(999)); // Default case
    }
}