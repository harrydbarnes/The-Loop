package com.example.theloop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.theloop.models.Article;
import com.example.theloop.models.CalendarEvent;
import com.example.theloop.models.NewsResponse;
import com.example.theloop.models.WeatherResponse;
import com.example.theloop.network.NewsApiService;
import com.example.theloop.network.NewsRetrofitClient;
import com.example.theloop.network.RetrofitClient;
import com.example.theloop.network.WeatherApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    static final String PREFS_NAME = "TheLoopPrefs";
    static final String KEY_FIRST_RUN = "is_first_run";
    static final String KEY_USER_NAME = "user_name";
    static final String KEY_NEWS_CATEGORY = "news_category";
    private static final String WEATHER_CACHE_KEY = "weather_cache";
    private static final String NEWS_CACHE_KEY = "news_cache";

    private static final String KEY_SECTION_ORDER = "section_order";
    private static final String SECTION_HEADLINES = "headlines";
    private static final String SECTION_CALENDAR = "calendar";
    private static final String SECTION_FUN_FACT = "fun_fact";
    private static final String DEFAULT_SECTION_ORDER = String.join(",", SECTION_HEADLINES, SECTION_CALENDAR, SECTION_FUN_FACT);
    private static final long CARD_ANIMATION_STAGGER_OFFSET_MS = 100L;


    // View variables...
    private TextView greetingTextView;
    private TextView summaryTextView;
    private ProgressBar weatherProgressBar;
    private TextView weatherErrorText;
    private LinearLayout weatherContentLayout;
    private ImageView weatherIcon;
    private TextView currentTemp;
    private TextView currentConditions;
    private TextView highLowTemp;
    private TextView dailyForecast;
    private FusedLocationProviderClient fusedLocationClient;
    private Gson gson = new Gson();
    private int selectedNewsCategoryIndex = 2; // Default to "general"
    private Runnable onLocationPermissionGranted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(KEY_FIRST_RUN, true);

        if (isFirstRun) {
            runSetupSequence();
        } else {
            loadDataForCurrentUser();
        }
    }

    private void runSetupSequence() {
        showNameDialog(() -> {
            requestLocationPermission(() -> {
                showNewsCategoryDialog(() -> {
                    // All setup steps are complete
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_FIRST_RUN, false).apply();
                    loadDataForCurrentUser();
                });
            });
        });
    }

    private void showNameDialog(Runnable onFinished) {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_enter_name, null);
        final EditText nameEditText = dialogView.findViewById(R.id.name_edit_text);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Welcome to The Loop!")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameEditText.getText().toString();
                    if (!TextUtils.isEmpty(name)) {
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_USER_NAME, name)
                                .apply();
                    }
                    onFinished.run();
                })
                .setCancelable(false)
                .show();
    }

    private void requestLocationPermission(Runnable onGranted) {
        this.onLocationPermissionGranted = onGranted;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (onLocationPermissionGranted != null) {
                onLocationPermissionGranted.run();
            }
        }
    }

    private void showNewsCategoryDialog(Runnable onFinished) {
        final String[] categories = {"Business", "Entertainment", "General", "Health", "Science", "Sports", "Technology"};

        new AlertDialog.Builder(this)
                .setTitle("Choose a News Category")
                .setSingleChoiceItems(categories, selectedNewsCategoryIndex, (dialog, which) -> {
                    selectedNewsCategoryIndex = which;
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedCategory = categories[selectedNewsCategoryIndex].toLowerCase(Locale.ROOT);
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_NEWS_CATEGORY, selectedCategory)
                            .apply();
                    onFinished.run();
                })
                .setCancelable(false)
                .show();
    }

    private void loadDataForCurrentUser() {
        updateDayAheadCard();
        fetchLocationAndThenWeatherData();
        setupCards();
    }

    private void setupCards() {
        LinearLayout cardsContainer = findViewById(R.id.cards_container);
        cardsContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String order = prefs.getString(KEY_SECTION_ORDER, DEFAULT_SECTION_ORDER);
        List<String> sections = Arrays.asList(order.split(","));

        for (int i = 0; i < sections.size(); i++) {
            String section = sections.get(i);
            View cardView = null;
            switch (section) {
                case SECTION_HEADLINES:
                    cardView = getLayoutInflater().inflate(R.layout.card_headlines, cardsContainer, false);
                    cardView.setTag(new HeadlinesViewHolder(cardView));
                    fetchNewsData(cardView);
                    break;
                case SECTION_CALENDAR:
                    cardView = getLayoutInflater().inflate(R.layout.card_calendar, cardsContainer, false);
                    cardView.setTag(SECTION_CALENDAR);
                    loadCalendarData(cardView);
                    break;
                case SECTION_FUN_FACT:
                    cardView = getLayoutInflater().inflate(R.layout.card_fun_fact, cardsContainer, false);
                    cardView.setTag(new FunFactViewHolder(cardView));
                    loadFunFact(cardView);
                    break;
            }
            if (cardView != null) {
                Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
                animation.setStartOffset(i * CARD_ANIMATION_STAGGER_OFFSET_MS);
                cardView.startAnimation(animation);
                cardsContainer.addView(cardView);
            }
        }
    }


    private void fetchLocationAndThenWeatherData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fetchWeatherData(37.77, -122.42); // Default: SF
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        fetchWeatherData(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.w(TAG, "Last location is null, using default.");
                        fetchWeatherData(37.77, -122.42); // Default: SF
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Failed to get location.", e);
                    fetchWeatherData(37.77, -122.42); // Default: SF
                });
    }

    private void initViews() {
        greetingTextView = findViewById(R.id.day_ahead_greeting);
        summaryTextView = findViewById(R.id.day_ahead_summary);
        weatherProgressBar = findViewById(R.id.weather_progress_bar);
        weatherErrorText = findViewById(R.id.weather_error_text);
        weatherContentLayout = findViewById(R.id.weather_content_layout);
        weatherIcon = findViewById(R.id.weather_icon);
        currentTemp = findViewById(R.id.current_temp);
        currentConditions = findViewById(R.id.current_conditions);
        highLowTemp = findViewById(R.id.high_low_temp);
        dailyForecast = findViewById(R.id.daily_forecast);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void fetchWeatherData(double latitude, double longitude) {
        if (!isNetworkAvailable()) {
            loadWeatherFromCache();
            return;
        }

        WeatherApiService apiService = RetrofitClient.getClient().create(WeatherApiService.class);
        String currentParams = "temperature_2m,weather_code";
        String dailyParams = "weather_code,temperature_2m_max,temperature_2m_min";
        Call<WeatherResponse> call = apiService.getWeather(latitude, longitude, currentParams, dailyParams, "fahrenheit", "auto");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                weatherProgressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    weatherContentLayout.setVisibility(View.VISIBLE);
                    Animation fadeIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in);
                    weatherContentLayout.startAnimation(fadeIn);
                    populateWeatherCard(response.body());
                    saveToCache(WEATHER_CACHE_KEY, response.body());
                } else {
                    loadWeatherFromCache();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Weather API call failed.", t);
                loadWeatherFromCache();
            }
        });
    }

    private void loadWeatherFromCache() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cachedJson = prefs.getString(WEATHER_CACHE_KEY, null);
        weatherProgressBar.setVisibility(View.GONE);
        if (cachedJson != null) {
            WeatherResponse cachedResponse = gson.fromJson(cachedJson, WeatherResponse.class);
            weatherContentLayout.setVisibility(View.VISIBLE);
            populateWeatherCard(cachedResponse);
        } else {
            weatherErrorText.setVisibility(View.VISIBLE);
        }
    }

    private void fetchNewsData(View cardView) {
        final HeadlinesViewHolder viewHolder = (HeadlinesViewHolder) cardView.getTag();

        if (!isNetworkAvailable()) {
            loadNewsFromCache(viewHolder);
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String category = prefs.getString(KEY_NEWS_CATEGORY, "general");

        NewsApiService apiService = NewsRetrofitClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getTopHeadlines(category, "us");

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                viewHolder.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null) {
                    populateHeadlinesCard(viewHolder, response.body().getArticles());
                    saveToCache(NEWS_CACHE_KEY, response.body());
                } else {
                    loadNewsFromCache(viewHolder);
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Log.e(TAG, "News API call failed.", t);
                loadNewsFromCache(viewHolder);
            }
        });
    }

    private void loadNewsFromCache(HeadlinesViewHolder viewHolder) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cachedJson = prefs.getString(NEWS_CACHE_KEY, null);
        viewHolder.progressBar.setVisibility(View.GONE);
        if (cachedJson != null) {
            NewsResponse cachedResponse = gson.fromJson(cachedJson, NewsResponse.class);
            if (cachedResponse != null && cachedResponse.getArticles() != null) {
                populateHeadlinesCard(viewHolder, cachedResponse.getArticles());
            } else {
                viewHolder.errorText.setVisibility(View.VISIBLE);
            }
        } else {
            viewHolder.errorText.setVisibility(View.VISIBLE);
        }
    }

    private void saveToCache(String key, Object data) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(data);
        editor.putString(key, json);
        editor.apply();
    }

    private void loadFunFact(View cardView) {
        final FunFactViewHolder viewHolder = (FunFactViewHolder) cardView.getTag();
        try {
            Resources res = getResources();
            String[] funFacts = res.getStringArray(R.array.fun_facts);
            Calendar calendar = Calendar.getInstance();
            int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
            int factIndex = dayOfYear % funFacts.length;
            viewHolder.funFactText.setText(funFacts[factIndex]);
        } catch (Exception e) {
            Log.e(TAG, "Could not load fun fact", e);
            viewHolder.funFactText.setText("Could not load a fun fact today. Try again later!");
        }
    }

    private void updateDayAheadCard() {
        greetingTextView.setText(getGreeting());
        summaryTextView.setText("A calm day ahead, with zero events on your calendar.");
    }

    private void loadCalendarData(View cardView) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_REQUEST_CODE);
        } else {
            queryCalendarEvents(cardView);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            LinearLayout cardsContainer = findViewById(R.id.cards_container);
            View calendarCard = cardsContainer.findViewWithTag(SECTION_CALENDAR);
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (calendarCard != null) {
                    queryCalendarEvents(calendarCard);
                }
            } else {
                if (calendarCard != null) {
                    calendarCard.findViewById(R.id.calendar_permission_denied_text).setVisibility(View.VISIBLE);
                    calendarCard.findViewById(R.id.calendar_events_container).setVisibility(View.GONE);
                    calendarCard.findViewById(R.id.calendar_no_events_text).setVisibility(View.GONE);
                }
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (onLocationPermissionGranted != null) {
                    onLocationPermissionGranted.run();
                }
            } else {
                Toast.makeText(this, "Location permission denied. Using default location for weather.", Toast.LENGTH_LONG).show();
                if (onLocationPermissionGranted != null) {
                    onLocationPermissionGranted.run();
                }
            }
        }
    }

    private void queryCalendarEvents(View cardView) {
        new Thread(() -> {
            List<CalendarEvent> events = new ArrayList<>();
            ContentResolver contentResolver = getContentResolver();
            Uri uri = CalendarContract.Events.CONTENT_URI;

            String[] projection = new String[]{
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_LOCATION
            };

            String selection = CalendarContract.Events.DTSTART + " >= ?";
            String[] selectionArgs = new String[]{String.valueOf(System.currentTimeMillis())};
            String sortOrder = CalendarContract.Events.DTSTART + " ASC";

            Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);

            if (cursor != null) {
                while (cursor.moveToNext() && events.size() < 3) {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE));
                    long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART));
                    long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND));
                    String location = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION));
                    events.add(new CalendarEvent(title, startTime, endTime, location));
                }
                cursor.close();
            }

            runOnUiThread(() -> populateCalendarCard(cardView, events));
        }).start();
    }

    private void populateCalendarCard(View cardView, List<CalendarEvent> events) {
        final CalendarViewHolder viewHolder = (CalendarViewHolder) cardView.getTag();

        viewHolder.eventsContainer.removeAllViews();
        if (events.isEmpty()) {
            viewHolder.noEventsText.setVisibility(View.VISIBLE);
            viewHolder.eventsContainer.setVisibility(View.GONE);
        } else {
            viewHolder.noEventsText.setVisibility(View.GONE);
            viewHolder.eventsContainer.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            viewHolder.eventsContainer.startAnimation(fadeIn);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (CalendarEvent event : events) {
                View eventView = inflater.inflate(R.layout.item_calendar_event, viewHolder.eventsContainer, false);
                TextView title = eventView.findViewById(R.id.event_title);
                TextView time = eventView.findViewById(R.id.event_time);
                TextView location = eventView.findViewById(R.id.event_location);

                title.setText(event.getTitle());
                time.setText(formatEventTime(event.getStartTime(), event.getEndTime()));

                if (!TextUtils.isEmpty(event.getLocation())) {
                    location.setText(event.getLocation());
                    location.setVisibility(View.VISIBLE);
                } else {
                    location.setVisibility(View.GONE);
                }
                viewHolder.eventsContainer.addView(eventView);
            }
        }
    }

    String formatEventTime(long startTime, long endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(startTime)) + " - " + sdf.format(new Date(endTime));
    }

    String getGreeting() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String name = prefs.getString(KEY_USER_NAME, "");

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (timeOfDay >= 0 && timeOfDay < 12) {
            greeting = "Good morning";
        } else if (timeOfDay >= 12 && timeOfDay < 17) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        if (!TextUtils.isEmpty(name)) {
            return greeting + ", " + name;
        } else {
            return greeting;
        }
    }

    void populateWeatherCard(WeatherResponse weather) {
        currentTemp.setText(String.format(Locale.getDefault(), "%.0f°F", weather.getCurrent().getTemperature()));
        currentConditions.setText(getWeatherDescription(weather.getCurrent().getWeatherCode()));
        weatherIcon.setImageResource(getWeatherIconResource(weather.getCurrent().getWeatherCode()));

        if (weather.getDaily().getTemperatureMax() != null && !weather.getDaily().getTemperatureMax().isEmpty()) {
            double maxTemp = weather.getDaily().getTemperatureMax().get(0);
            double minTemp = weather.getDaily().getTemperatureMin().get(0);
            highLowTemp.setText(String.format(Locale.getDefault(), "H:%.0f° L:%.0f°", maxTemp, minTemp));
            dailyForecast.setText(getDailyForecast(weather.getDaily().getWeatherCode().get(0)));
        }
    }

    void populateHeadlinesCard(View cardView, List<Article> articles) {
        final HeadlinesViewHolder viewHolder = (HeadlinesViewHolder) cardView.getTag();
        viewHolder.container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = 0;
        for (Article article : articles) {
            if (count >= 3) break;

            View headlineView = inflater.inflate(R.layout.item_headline, viewHolder.container, false);
            TextView title = headlineView.findViewById(R.id.headline_title);
            TextView sourceTime = headlineView.findViewById(R.id.headline_source_time);

            title.setText(article.getTitle());
            String sourceAndTimeText = article.getSource().getName() + " • " + formatPublishedAt(article.getPublishedAt());
            sourceTime.setText(sourceAndTimeText);

            headlineView.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
                startActivity(browserIntent);
            });

            viewHolder.container.addView(headlineView);
            count++;
        }
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        viewHolder.container.startAnimation(fadeIn);
    }

    String formatPublishedAt(String publishedAt) {
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
            long hoursAgo = ChronoUnit.HOURS.between(zdt, ZonedDateTime.now());
            if (hoursAgo < 1) {
                long minutesAgo = ChronoUnit.MINUTES.between(zdt, ZonedDateTime.now());
                return minutesAgo + "m ago";
            } else if (hoursAgo < 24) {
                return hoursAgo + "h ago";
            } else {
                long daysAgo = ChronoUnit.DAYS.between(zdt, ZonedDateTime.now());
                return daysAgo + "d ago";
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not parse date: " + publishedAt, e);
            return "Just now";
        }
    }

    String getWeatherDescription(int weatherCode) {
        switch (weatherCode) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Fog";
            case 51: case 53: case 55: return "Drizzle";
            case 61: case 63: case 65: return "Rain";
            case 71: case 73: case 75: return "Snow fall";
            case 80: case 81: case 82: return "Rain showers";
            case 95: return "Thunderstorm";
            default: return "Unknown";
        }
    }

    String getDailyForecast(int weatherCode) {
        switch (weatherCode) {
            case 0: return "Expect clear skies today.";
            case 1: return "Mainly clear skies expected.";
            case 2: return "Partly cloudy today.";
            case 3: return "Expect overcast skies.";
            case 45: case 48: return "Fog is expected today.";
            case 51: case 53: case 55: return "Light drizzle possible.";
            case 61: case 63: case 65: return "Rain expected today.";
            case 71: case 73: case 75: return "Snowfall is expected.";
            case 80: case 81: case 82: return "Expect rain showers.";
            case 95: return "Thunderstorms possible.";
            default: return "Weather data unavailable.";
        }
    }

    int getWeatherIconResource(int weatherCode) {
        switch (weatherCode) {
            case 0: return R.drawable.ic_weather_sunny;
            case 1: case 2: return R.drawable.ic_weather_partly_cloudy;
            case 3: return R.drawable.ic_weather_cloudy;
            case 45: case 48: return R.drawable.ic_weather_foggy;
            case 51: case 53: case 55: case 61: case 63: case 65: case 80: case 81: case 82:
                return R.drawable.ic_weather_rainy;
            case 71: case 73: case 75: case 85: case 86:
                return R.drawable.ic_weather_snowy;
            case 95: case 96: case 99:
                return R.drawable.ic_weather_thunderstorm;
            default:
                return R.drawable.ic_weather_cloudy;
        }
    }

    private static class HeadlinesViewHolder {
        final ProgressBar progressBar;
        final TextView errorText;
        final LinearLayout container;

        HeadlinesViewHolder(View cardView) {
            progressBar = cardView.findViewById(R.id.headlines_progress_bar);
            errorText = cardView.findViewById(R.id.headlines_error_text);
            container = cardView.findViewById(R.id.headlines_container);
        }
    }

    private static class CalendarViewHolder {
        final TextView permissionDeniedText;
        final TextView noEventsText;
        final LinearLayout eventsContainer;

        CalendarViewHolder(View cardView) {
            permissionDeniedText = cardView.findViewById(R.id.calendar_permission_denied_text);
            noEventsText = cardView.findViewById(R.id.calendar_no_events_text);
            eventsContainer = cardView.findViewById(R.id.calendar_events_container);
        }
    }

    private static class FunFactViewHolder {
        final TextView funFactText;

        FunFactViewHolder(View cardView) {
            funFactText = cardView.findViewById(R.id.fun_fact_text);
        }
    }
}