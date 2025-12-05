package com.example.theloop;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.theloop.models.Article;
import com.example.theloop.models.CalendarEvent;
import com.example.theloop.models.NewsResponse;
import com.example.theloop.models.WeatherResponse;
import com.example.theloop.network.NewsApiService;
import com.example.theloop.network.NewsRetrofitClient;
import com.example.theloop.network.RetrofitClient;
import com.example.theloop.network.WeatherApiService;
import com.example.theloop.utils.AppUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final long CARD_ANIMATION_STAGGER_DELAY_MS = 100L;
    static final String PREFS_NAME = "TheLoopPrefs";
    static final String KEY_FIRST_RUN = "is_first_run";
    static final String KEY_USER_NAME = "user_name";
    static final String KEY_TEMP_UNIT = "temp_unit"; // celsius or fahrenheit
    private static final String WEATHER_CACHE_KEY = "weather_cache";
    private static final String NEWS_CACHE_KEY = "news_cache";
    private static final String KEY_SECTION_ORDER = "section_order";

    private static final String SECTION_HEADLINES = "headlines";
    private static final String SECTION_CALENDAR = "calendar";
    private static final String SECTION_FUN_FACT = "fun_fact";
    private static final String DEFAULT_SECTION_ORDER = SECTION_HEADLINES + "," + SECTION_CALENDAR + "," + SECTION_FUN_FACT;

    private static final double DEFAULT_LATITUDE = 51.5480; // Highbury, London
    private static final double DEFAULT_LONGITUDE = -0.1030; // Highbury, London

    private static final String PENDING_CALENDAR_CARD_TAG_KEY = "pending_calendar_card_tag";

    private static final java.time.format.DateTimeFormatter WEATHER_DATE_INPUT_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    private static final java.time.format.DateTimeFormatter WEATHER_DATE_DAY_FORMAT = java.time.format.DateTimeFormatter.ofPattern("EEE d", Locale.getDefault());

    // Static View variables (Day Ahead and Weather)
    private TextView greetingTextView;
    private TextView summaryTextView;
    private ProgressBar weatherProgressBar;
    private TextView weatherErrorText;
    private LinearLayout weatherContentLayout;
    private ImageView weatherIcon;
    private TextView currentTemp;
    private TextView currentConditions;
    private TextView highLowTemp;
    private LinearLayout dailyForecastContainer;
    private TextView weatherLocation;
    private ImageView weatherSettingsIcon;

    private Gson gson = new Gson();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;
    private int selectedNewsCategory = R.id.chip_us; // Default category ID
    private NewsResponse cachedNewsResponse;
    private Runnable onLocationPermissionGranted;
    private String pendingCalendarCardTag;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static class HeadlinesViewHolder {
        final ProgressBar progressBar;
        final TextView errorText;
        final LinearLayout container;
        final ChipGroup chipGroup;

        HeadlinesViewHolder(View cardView) {
            progressBar = cardView.findViewById(R.id.headlines_progress_bar);
            errorText = cardView.findViewById(R.id.headlines_error_text);
            container = cardView.findViewById(R.id.headlines_container);
            chipGroup = cardView.findViewById(R.id.headlines_category_chips);
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCalendarCardTag != null) {
            outState.putString(PENDING_CALENDAR_CARD_TAG_KEY, pendingCalendarCardTag);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            pendingCalendarCardTag = savedInstanceState.getString(PENDING_CALENDAR_CARD_TAG_KEY);
        }

        initViews();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
if (Geocoder.isPresent()) {
    geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
} else {
    Log.w(TAG, "Geocoder service not available, location name will not be shown.");
}

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(KEY_FIRST_RUN, true);

        if (isFirstRun) {
            runSetupSequence();
        } else {
            setupCards();
        }

        weatherSettingsIcon.setOnClickListener(v -> showTemperatureUnitDialog());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void runSetupSequence() {
        showNameDialog(this::onNameEntered);
    }

    private void onNameEntered() {
        requestLocationPermission(this::onLocationPermissionGrantedForSetup);
    }

    private void onLocationPermissionGrantedForSetup() {
        // We no longer ask for news category in setup as it's selectable in the UI
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_FIRST_RUN, false).apply();
        setupCards();
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

    private void showTemperatureUnitDialog() {
        String[] unitsDisplay = getResources().getStringArray(R.array.temp_units_display);
        String[] unitsValues = getResources().getStringArray(R.array.temp_units_values);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUnit = prefs.getString(KEY_TEMP_UNIT, unitsValues[0]);
        int checkedItem = Math.max(0, Arrays.asList(unitsValues).indexOf(currentUnit));

        new AlertDialog.Builder(this)
            .setTitle(R.string.select_temperature_unit)
            .setSingleChoiceItems(unitsDisplay, checkedItem, (dialog, which) -> {
                String selected = unitsValues[which];
                prefs.edit().putString(KEY_TEMP_UNIT, selected).apply();
                dialog.dismiss();
                fetchLocationAndThenWeatherData(); // Refresh weather
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void setupCards() {
        updateDayAheadCard();
        fetchLocationAndThenWeatherData();

        LinearLayout cardsContainer = findViewById(R.id.cards_container);
        cardsContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String order = prefs.getString(KEY_SECTION_ORDER, DEFAULT_SECTION_ORDER);
        List<String> sections = new ArrayList<>(Arrays.asList(order.split(",")));

        for (int i = 0; i < sections.size(); i++) {
            View cardView = null;
            String section = sections.get(i);
            switch (section) {
                case SECTION_HEADLINES -> {
                    cardView = getLayoutInflater().inflate(R.layout.card_headlines, cardsContainer, false);
                    cardView.setTag(R.id.view_holder_tag, new HeadlinesViewHolder(cardView));
                    setupHeadlinesCard(cardView);
                    fetchNewsData(cardView);
                }
                case SECTION_CALENDAR -> {
                    cardView = getLayoutInflater().inflate(R.layout.card_calendar, cardsContainer, false);
                    String calendarTag = "calendar_card_" + i;
                    cardView.setTag(calendarTag);
                    cardView.setTag(R.id.view_holder_tag, new CalendarViewHolder(cardView));
                    loadCalendarData(cardView);
                }
                case SECTION_FUN_FACT -> {
                    cardView = getLayoutInflater().inflate(R.layout.card_fun_fact, cardsContainer, false);
                    cardView.setTag(R.id.view_holder_tag, new FunFactViewHolder(cardView));
                    loadFunFact(cardView);
                }
                default -> Log.w(TAG, "Unknown section type: " + section);
            }
            if (cardView != null) {
                Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
                animation.setStartOffset(i * CARD_ANIMATION_STAGGER_DELAY_MS);
                cardView.startAnimation(animation);
                cardsContainer.addView(cardView);
            }
        }
    }

    private void setupHeadlinesCard(View cardView) {
        if (!(cardView.getTag(R.id.view_holder_tag) instanceof HeadlinesViewHolder)) {
            Log.e(TAG, "Invalid ViewHolder tag in setupHeadlinesCard");
            return;
        }
        HeadlinesViewHolder holder = (HeadlinesViewHolder) cardView.getTag(R.id.view_holder_tag);
        holder.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) return;

            selectedNewsCategory = checkedId;
            if (cachedNewsResponse != null) {
                displayNewsForCategory(cardView, cachedNewsResponse);
            } else {
                fetchNewsData(cardView);
            }
        });
        // Default selection
        holder.chipGroup.check(R.id.chip_us);
    }

    private void fetchWeatherForDefaultLocation() {
        fetchWeatherData(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
        updateLocationName(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
    }

    private void fetchLocationAndThenWeatherData() {
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fetchWeatherForDefaultLocation();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        fetchWeatherData(location.getLatitude(), location.getLongitude());
                        updateLocationName(location.getLatitude(), location.getLongitude());
                    } else {
                        fetchWeatherForDefaultLocation();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Failed to get location.", e);
                    fetchWeatherForDefaultLocation();
                });
    }

    private void updateLocationName(double lat, double lon) {
        Runnable setUnknownLocation = () -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            weatherLocation.setText(getString(R.string.unknown_location));
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lon, 1, addresses -> {
                if (addresses != null && !addresses.isEmpty()) {
                    processLocationAddresses(addresses);
                } else {
                    setUnknownLocation.run();
                }
            });
        } else {
            executorService.execute(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        processLocationAddresses(addresses);
                    } else {
                        setUnknownLocation.run();
                    }
                } catch (java.io.IOException e) {
                    Log.e(TAG, "Geocoder failed", e);
                    setUnknownLocation.run();
                }
            });
        }
    }

    private void processLocationAddresses(List<Address> addresses) {
        String city = addresses.get(0).getLocality();
        if (TextUtils.isEmpty(city)) {
            city = addresses.get(0).getSubAdminArea();
        }

        final String finalCity = TextUtils.isEmpty(city) ? getString(R.string.unknown_location) : city;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            weatherLocation.setText(finalCity);
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
        dailyForecastContainer = findViewById(R.id.daily_forecast_container);
        weatherLocation = findViewById(R.id.weather_location);
        weatherSettingsIcon = findViewById(R.id.weather_settings_icon);
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

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String unit = prefs.getString(KEY_TEMP_UNIT, getResources().getStringArray(R.array.temp_units_values)[0]);

        WeatherApiService apiService = RetrofitClient.getClient().create(WeatherApiService.class);
        String currentParams = "temperature_2m,weather_code";
        String dailyParams = "weather_code,temperature_2m_max,temperature_2m_min";
        Call<WeatherResponse> call = apiService.getWeather(latitude, longitude, currentParams, dailyParams, unit, "auto");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
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
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
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
        if (cardView == null) {
            Log.e(TAG, "CardView is null in fetchNewsData");
            return;
        }
        Object tag = cardView.getTag(R.id.view_holder_tag);
        if (!(tag instanceof HeadlinesViewHolder)) {
             Log.e(TAG, "ViewHolder is not of type HeadlinesViewHolder in fetchNewsData");
             return;
        }
        HeadlinesViewHolder holder = (HeadlinesViewHolder) tag;
        if (!isNetworkAvailable()) {
            loadNewsFromCache(cardView);
            return;
        }

        NewsApiService apiService = NewsRetrofitClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getNewsFeed(); // Fetch full feed

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                holder.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    cachedNewsResponse = response.body();
                    displayNewsForCategory(cardView, cachedNewsResponse);
                    saveToCache(NEWS_CACHE_KEY, response.body());
                } else {
                    loadNewsFromCache(cardView);
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Log.e(TAG, "News API call failed.", t);
                loadNewsFromCache(cardView);
            }
        });
    }

    private void displayNewsForCategory(View cardView, NewsResponse response) {
        if (cardView == null) return;
        Object tag = cardView.getTag(R.id.view_holder_tag);
        if (!(tag instanceof HeadlinesViewHolder)) {
            Log.e(TAG, "Invalid ViewHolder tag in displayNewsForCategory");
            return;
        }
        HeadlinesViewHolder holder = (HeadlinesViewHolder) tag;

        List<Article> articles = switch (selectedNewsCategory) {
            case R.id.chip_business -> response.getBusiness();
            case R.id.chip_entertainment -> response.getEntertainment();
            case R.id.chip_health -> response.getHealth();
            case R.id.chip_science -> response.getScience();
            case R.id.chip_sports -> response.getSports();
            case R.id.chip_technology -> response.getTechnology();
            case R.id.chip_world -> response.getWorld();
            default -> response.getUs(); // "US" or default
        };

        if (articles != null) {
            populateHeadlinesCard(cardView, articles);
        } else {
             holder.errorText.setVisibility(View.VISIBLE);
        }
    }

    private void loadNewsFromCache(View cardView) {
        if (cardView == null) return;
        Object tag = cardView.getTag(R.id.view_holder_tag);
        if (!(tag instanceof HeadlinesViewHolder)) {
            Log.e(TAG, "Invalid ViewHolder tag in loadNewsFromCache");
            return;
        }
        HeadlinesViewHolder holder = (HeadlinesViewHolder) tag;
        holder.progressBar.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cachedJson = prefs.getString(NEWS_CACHE_KEY, null);
        if (cachedJson != null) {
            NewsResponse cachedResponse = gson.fromJson(cachedJson, NewsResponse.class);
            if (cachedResponse != null) {
                this.cachedNewsResponse = cachedResponse;
                displayNewsForCategory(cardView, cachedResponse);
            } else {
                holder.errorText.setVisibility(View.VISIBLE);
            }
        } else {
            holder.errorText.setVisibility(View.VISIBLE);
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
        if (cardView == null) {
            Log.e(TAG, "CardView is null in loadFunFact");
            return;
        }
        Object tag = cardView.getTag(R.id.view_holder_tag);
        if (!(tag instanceof FunFactViewHolder)) {
            Log.e(TAG, "ViewHolder is not of type FunFactViewHolder in loadFunFact");
            return;
        }
        FunFactViewHolder holder = (FunFactViewHolder) tag;
        try {
            Resources res = getResources();
            String[] funFacts = res.getStringArray(R.array.fun_facts);
            Calendar calendar = Calendar.getInstance();
            int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
            int factIndex = dayOfYear % funFacts.length;
            holder.funFactText.setText(funFacts[factIndex]);
        } catch (Exception e) {
            Log.e(TAG, "Could not load fun fact", e);
            holder.funFactText.setText("Could not load a fun fact today. Try again later!");
        }
    }

    private void updateDayAheadCard() {
        greetingTextView.setText(getGreeting());
        summaryTextView.setText("A calm day ahead, with zero events on your calendar.");
    }

    private void loadCalendarData(View cardView) {
        if (cardView == null) {
            Log.e(TAG, "CardView is null in loadCalendarData");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Object tag = cardView.getTag();
            if (tag instanceof String) {
                pendingCalendarCardTag = (String) tag;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_REQUEST_CODE);
            } else {
                Log.e(TAG, "Calendar card does not have a valid string tag for permission request.");
            }
        } else {
            queryCalendarEvents(cardView);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCalendarCardTag != null) {
                    View calendarCard = findViewById(R.id.cards_container).findViewWithTag(pendingCalendarCardTag);
                    if (calendarCard != null) {
                        queryCalendarEvents(calendarCard);
                    }
                }
            } else {
                if (pendingCalendarCardTag != null) {
                    View calendarCard = findViewById(R.id.cards_container).findViewWithTag(pendingCalendarCardTag);
                    if (calendarCard != null) {
                        Object tag = calendarCard.getTag(R.id.view_holder_tag);
                        if (tag instanceof CalendarViewHolder) {
                            CalendarViewHolder holder = (CalendarViewHolder) tag;
                            holder.permissionDeniedText.setVisibility(View.VISIBLE);
                        } else {
                            Log.e(TAG, "Could not find CalendarViewHolder for tag: " + pendingCalendarCardTag);
                        }
                    }
                }
            }
            pendingCalendarCardTag = null;
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
        try {
            executorService.execute(() -> {
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

                String selection = CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTSTART + " <= ?";
                long now = System.currentTimeMillis();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(now);
                cal.add(Calendar.HOUR_OF_DAY, 24);
                long queryCutoffTime = cal.getTimeInMillis();

                String[] selectionArgs = new String[]{String.valueOf(now), String.valueOf(queryCutoffTime)};
                String sortOrder = CalendarContract.Events.DTSTART + " ASC";

                try (Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)) {
                    if (cursor != null) {
                        try {
                            int idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID);
                            int titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE);
                            int startCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART);
                            int endCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND);
                            int locationCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION);
                            while (cursor.moveToNext() && events.size() < 3) {
                                long id = cursor.getLong(idCol);
                                String title = cursor.getString(titleCol);
                                long startTime = cursor.getLong(startCol);
                                long endTime = cursor.getLong(endCol);
                                String location = cursor.getString(locationCol);
                                events.add(new CalendarEvent(id, title, startTime, endTime, location));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing calendar cursor", e);
                        }
                    }
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    populateCalendarCard(cardView, events);
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error executing calendar query", e);
        }
    }

    private void populateCalendarCard(View cardView, List<CalendarEvent> events) {
        if (cardView == null) {
            Log.e(TAG, "CardView is null in populateCalendarCard");
            return;
        }
        Object tag = cardView.getTag(R.id.view_holder_tag);
        if (!(tag instanceof CalendarViewHolder)) {
            Log.e(TAG, "ViewHolder is not of type CalendarViewHolder in populateCalendarCard");
            return;
        }
        CalendarViewHolder holder = (CalendarViewHolder) tag;
        holder.permissionDeniedText.setVisibility(View.GONE);
        holder.eventsContainer.removeAllViews();
        if (events.isEmpty()) {
            holder.noEventsText.setVisibility(View.VISIBLE);
            holder.eventsContainer.setVisibility(View.GONE);
        } else {
            holder.noEventsText.setVisibility(View.GONE);
            holder.eventsContainer.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            holder.eventsContainer.startAnimation(fadeIn);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (CalendarEvent event : events) {
                View eventView = inflater.inflate(R.layout.item_calendar_event, holder.eventsContainer, false);
                TextView title = eventView.findViewById(R.id.event_title);
                TextView time = eventView.findViewById(R.id.event_time);
                TextView location = eventView.findViewById(R.id.event_location);

                title.setText(event.getTitle());
                time.setText(AppUtils.formatEventTime(this, event.getStartTime(), event.getEndTime()));

                if (!TextUtils.isEmpty(event.getLocation())) {
                    location.setText(event.getLocation());
                    location.setVisibility(View.VISIBLE);
                } else {
                    location.setVisibility(View.GONE);
                }

                eventView.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.getId());
                    Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(v.getContext(), "No app found to open calendar event.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to open calendar event", e);
                    }
                });

                holder.eventsContainer.addView(eventView);
            }
        }
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
            return String.format("%s, %s", greeting, name);
        } else {
            return greeting;
        }
    }

    void populateWeatherCard(WeatherResponse weather) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String unit = prefs.getString(KEY_TEMP_UNIT, getResources().getStringArray(R.array.temp_units_values)[0]);
        String tempSymbol = unit.equals("celsius") ? "°C" : "°F";

        currentTemp.setText(String.format(Locale.getDefault(), "%.0f%s", weather.getCurrent().getTemperature(), tempSymbol));
        currentConditions.setText(getString(AppUtils.getWeatherDescription(weather.getCurrent().getWeatherCode())));
        weatherIcon.setImageResource(AppUtils.getWeatherIconResource(weather.getCurrent().getWeatherCode()));

        com.example.theloop.models.DailyWeather daily = weather.getDaily();
        if (daily != null && daily.getTemperatureMax() != null && daily.getTemperatureMin() != null
                && daily.getWeatherCode() != null && daily.getTime() != null) {

            // Populate 5-day forecast
            dailyForecastContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            int minSize = Math.min(daily.getTime().size(),
                    Math.min(daily.getTemperatureMax().size(),
                            Math.min(daily.getTemperatureMin().size(), daily.getWeatherCode().size())));
            int daysToShow = Math.min(5, minSize);

            if (minSize > 0) {
                double maxTemp = daily.getTemperatureMax().get(0);
                double minTemp = daily.getTemperatureMin().get(0);
                highLowTemp.setText(String.format(Locale.getDefault(), "H:%.0f%s L:%.0f%s", maxTemp, tempSymbol, minTemp, tempSymbol));
            }

            for (int i = 0; i < daysToShow; i++) {
                View forecastView = inflater.inflate(R.layout.item_daily_forecast, dailyForecastContainer, false);
                TextView dayText = forecastView.findViewById(R.id.forecast_day);
                ImageView icon = forecastView.findViewById(R.id.forecast_icon);
                TextView high = forecastView.findViewById(R.id.forecast_high);
                TextView low = forecastView.findViewById(R.id.forecast_low);

                try {
                    java.time.LocalDate date = java.time.LocalDate.parse(daily.getTime().get(i), WEATHER_DATE_INPUT_FORMAT);
                    dayText.setText(date.format(WEATHER_DATE_DAY_FORMAT));
                } catch (java.time.format.DateTimeParseException e) {
                    Log.e(TAG, "Error parsing weather date", e);
                    dayText.setText("-");
                }

                icon.setImageResource(AppUtils.getWeatherIconResource(daily.getWeatherCode().get(i)));
                high.setText(String.format(Locale.getDefault(), "%.0f%s", daily.getTemperatureMax().get(i), tempSymbol));
                low.setText(String.format(Locale.getDefault(), "%.0f%s", daily.getTemperatureMin().get(i), tempSymbol));

                dailyForecastContainer.addView(forecastView);
            }
        }
    }

    void populateHeadlinesCard(View cardView, List<Article> articles) {
        if (cardView == null) {
            Log.e(TAG, "CardView is null in populateHeadlinesCard");
            return;
        }
        Object tag = cardView.getTag(R.id.view_holder_tag);
        if (!(tag instanceof HeadlinesViewHolder)) {
             Log.e(TAG, "ViewHolder is not of type HeadlinesViewHolder in populateHeadlinesCard");
             return;
        }
        HeadlinesViewHolder holder = (HeadlinesViewHolder) tag;
        holder.container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = 0;
        for (Article article : articles) {
            if (count >= 3) break;

            View headlineView = inflater.inflate(R.layout.item_headline, holder.container, false);
            TextView title = headlineView.findViewById(R.id.headline_title);
            TextView sourceTextView = headlineView.findViewById(R.id.headline_source_time);

            title.setText(article.getTitle());
            String sourceText = article.getSource();
            sourceTextView.setText(sourceText);

            headlineView.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
                try {
                    startActivity(browserIntent);
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(v.getContext(), "No browser found to open link.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to open article link", e);
                }
            });

            holder.container.addView(headlineView);
            count++;
        }
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        holder.container.startAnimation(fadeIn);
    }
}