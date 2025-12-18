package com.example.theloop;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.example.theloop.models.Article;
import com.example.theloop.models.CalendarEvent;
import com.example.theloop.models.FunFactResponse;
import com.example.theloop.models.NewsResponse;
import com.example.theloop.models.WeatherResponse;
import com.example.theloop.network.FunFactApiService;
import com.example.theloop.network.FunFactRetrofitClient;
import com.example.theloop.network.NewsApiService;
import com.example.theloop.network.NewsRetrofitClient;
import com.example.theloop.network.RetrofitClient;
import com.example.theloop.network.WeatherApiService;
import com.example.theloop.utils.AppConstants;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";
    private final Gson gson = new Gson();

    private final MutableLiveData<WeatherResponse> _latestWeather = new MutableLiveData<>();
    public LiveData<WeatherResponse> latestWeather = _latestWeather;

    private final MutableLiveData<NewsResponse> _cachedNewsResponse = new MutableLiveData<>();
    public LiveData<NewsResponse> cachedNewsResponse = _cachedNewsResponse;

    private final MutableLiveData<String> _funFactText = new MutableLiveData<>();
    public LiveData<String> funFactText = _funFactText;

    private final MutableLiveData<List<CalendarEvent>> _calendarEvents = new MutableLiveData<>();
    public LiveData<List<CalendarEvent>> calendarEvents = _calendarEvents;

    private final MutableLiveData<Integer> _totalEventCount = new MutableLiveData<>(0);
    public LiveData<Integer> totalEventCount = _totalEventCount;

    private final MutableLiveData<Boolean> _calendarQueryError = new MutableLiveData<>(false);
    public LiveData<Boolean> calendarQueryError = _calendarQueryError;

    // Call objects to cancel on clear
    private Call<WeatherResponse> weatherCall;
    private Call<NewsResponse> newsCall;
    private Call<FunFactResponse> funFactCall;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String[] CALENDAR_PROJECTION = new String[]{
            CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND, CalendarContract.Events.EVENT_LOCATION
    };

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public void fetchWeatherData(double latitude, double longitude) {
        // No explicit network check here, relying on Repository pattern or Caller to handle checks or failure
        // But for this refactor, we keep logic similar to MainActivity but inside ViewModel
        // Ideally we would inject a Repository.

        SharedPreferences prefs = getApplication().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        String unit = prefs.getString(AppConstants.KEY_TEMP_UNIT, getApplication().getResources().getStringArray(R.array.temp_units_values)[0]);

        WeatherApiService apiService = RetrofitClient.getClient().create(WeatherApiService.class);
        weatherCall = apiService.getWeather(latitude, longitude, "temperature_2m,weather_code", "weather_code,temperature_2m_max,temperature_2m_min", unit, "auto");

        weatherCall.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _latestWeather.postValue(response.body());
                    saveToCache(AppConstants.WEATHER_CACHE_KEY, response.body());
                } else {
                    Log.e(TAG, "Weather API response not successful: " + response.code());
                    onFailure(call, new java.io.IOException("API response not successful: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Weather failed", t);
                // Load from cache if network fails
                loadWeatherFromCache();
            }
        });
    }

    public void loadWeatherFromCache() {
        SharedPreferences prefs = getApplication().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        String cachedJson = prefs.getString(AppConstants.WEATHER_CACHE_KEY, null);
        if (cachedJson != null) {
            try {
                WeatherResponse weather = gson.fromJson(cachedJson, WeatherResponse.class);
                _latestWeather.postValue(weather);
            } catch (Exception e) {
                 Log.e(TAG, "Failed to load weather from cache", e);
            }
        }
    }

    public void fetchNewsData() {
        NewsApiService apiService = NewsRetrofitClient.getClient().create(NewsApiService.class);
        newsCall = apiService.getNewsFeed();
        newsCall.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _cachedNewsResponse.postValue(response.body());
                    saveToCache(AppConstants.NEWS_CACHE_KEY, response.body());
                } else {
                    Log.e(TAG, "News API response not successful: " + response.code());
                    onFailure(call, new java.io.IOException("API response not successful: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "News API call failed.", t);
                loadNewsFromCache();
            }
        });
    }

    public void loadNewsFromCache() {
        SharedPreferences prefs = getApplication().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        String cachedJson = prefs.getString(AppConstants.NEWS_CACHE_KEY, null);
        if (cachedJson != null) {
            try {
                NewsResponse news = gson.fromJson(cachedJson, NewsResponse.class);
                _cachedNewsResponse.postValue(news);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load news from cache", e);
            }
        }
    }

    public void fetchFunFact() {
        FunFactApiService api = FunFactRetrofitClient.getClient().create(FunFactApiService.class);
        funFactCall = api.getRandomFact("en");
        funFactCall.enqueue(new Callback<FunFactResponse>() {
            @Override
            public void onResponse(Call<FunFactResponse> call, Response<FunFactResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _funFactText.postValue(response.body().getText());
                } else {
                    loadFallbackFunFact();
                }
            }

            @Override
            public void onFailure(Call<FunFactResponse> call, Throwable t) {
                loadFallbackFunFact();
            }
        });
    }

    public void loadFallbackFunFact() {
        try {
            String[] facts = getApplication().getResources().getStringArray(R.array.fun_facts);
            int idx = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) % facts.length;
            _funFactText.postValue(facts[idx]);
        } catch (Exception e) {
            _funFactText.postValue("Did you know? Code is poetry.");
        }
    }

    public void loadCalendarData() {
        executorService.execute(() -> {
            _calendarQueryError.postValue(false);
            List<CalendarEvent> events = new ArrayList<>();
            try {
                ContentResolver contentResolver = getApplication().getContentResolver();
                Uri uri = CalendarContract.Events.CONTENT_URI;
                long now = System.currentTimeMillis();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(now);
                cal.add(Calendar.HOUR_OF_DAY, 24);
                long end = cal.getTimeInMillis();

                String selection = CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTSTART + " <= ?";
                String[] selectionArgs = new String[]{String.valueOf(now), String.valueOf(end)};
                String sort = CalendarContract.Events.DTSTART + " ASC";

                try (Cursor cursor = contentResolver.query(uri, CALENDAR_PROJECTION, selection, selectionArgs, sort)) {
                    if (cursor != null) {
                        _totalEventCount.postValue(cursor.getCount());
                        int idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID);
                        int titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE);
                        int startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART);
                        int endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND);
                        int locIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION);

                        while (cursor.moveToNext() && events.size() < 3) {
                            events.add(new CalendarEvent(
                                cursor.getLong(idIdx), cursor.getString(titleIdx), cursor.getLong(startIdx), cursor.getLong(endIdx), cursor.getString(locIdx)
                            ));
                        }
                    } else {
                        _totalEventCount.postValue(0);
                    }
                }
                _calendarEvents.postValue(events);
            } catch (Exception e) {
                Log.e(TAG, "Cal error", e);
                _calendarQueryError.postValue(true);
                _calendarEvents.postValue(Collections.emptyList());
            }
        });
    }

    public void saveSummaryToCache(String summary) {
        getApplication().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(AppConstants.KEY_SUMMARY_CACHE, summary).apply();
    }

    private void saveToCache(String key, Object data) {
        getApplication().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(key, gson.toJson(data)).apply();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        if (weatherCall != null) weatherCall.cancel();
        if (newsCall != null) newsCall.cancel();
        if (funFactCall != null) funFactCall.cancel();
    }
}
