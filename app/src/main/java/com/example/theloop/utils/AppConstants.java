package com.example.theloop.utils;

public final class AppConstants {
    private AppConstants() {
        // Restricted instantiation
    }

    public static final String PREFS_NAME = "TheLoopPrefs";
    public static final String KEY_LATITUDE = "last_latitude";
    public static final String KEY_LONGITUDE = "last_longitude";
    public static final String KEY_TEMP_UNIT = "temp_unit";
    public static final String WEATHER_CACHE_KEY = "weather_cache";
    public static final String NEWS_CACHE_KEY = "news_cache";
    public static final String KEY_SECTION_ORDER = "section_order";
    public static final String KEY_SUMMARY_CACHE = "summary_cache";

    public static final double DEFAULT_LATITUDE = 51.5480;
    public static final double DEFAULT_LONGITUDE = -0.1030;
}
