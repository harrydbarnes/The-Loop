package com.example.theloop.utils;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

import com.example.theloop.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class AppUtilsTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void getWeatherDescription_mapsCodesCorrectly() {
        assertEquals(R.string.weather_clear_sky, AppUtils.getWeatherDescription(0));
        assertEquals(R.string.weather_partly_cloudy, AppUtils.getWeatherDescription(2));
        assertEquals(R.string.weather_fog, AppUtils.getWeatherDescription(45));
        assertEquals(R.string.weather_rain, AppUtils.getWeatherDescription(61));
        assertEquals(R.string.weather_snow_fall, AppUtils.getWeatherDescription(75));
        assertEquals(R.string.weather_thunderstorm, AppUtils.getWeatherDescription(95));
        assertEquals(R.string.weather_thunderstorm_with_hail, AppUtils.getWeatherDescription(96));
        assertEquals(R.string.weather_thunderstorm_with_hail, AppUtils.getWeatherDescription(99));
        assertEquals(R.string.weather_unknown, AppUtils.getWeatherDescription(1000));
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

    @Test
    public void formatForecastDates_formatsDatesCorrectly() {
        List<String> rawDates = Arrays.asList("2023-10-25", "2023-10-26", "invalid-date");
        List<String> formatted = AppUtils.formatForecastDates(rawDates);

        assertEquals(3, formatted.size());

        // Verify success cases dynamically to avoid locale-dependent failures
        DateTimeFormatter testFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault());
        assertEquals(LocalDate.parse("2023-10-25").format(testFormatter), formatted.get(0));
        assertEquals(LocalDate.parse("2023-10-26").format(testFormatter), formatted.get(1));
        assertEquals("-", formatted.get(2));
    }
}
