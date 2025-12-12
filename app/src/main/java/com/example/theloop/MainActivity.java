package com.example.theloop;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import android.speech.tts.TextToSpeech;
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
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.AggregateRequest;
import androidx.health.connect.client.time.TimeRangeFilter;
import androidx.health.connect.client.PermissionController;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.theloop.models.Article;
import com.example.theloop.models.CalendarEvent;
import com.example.theloop.models.FunFactResponse;
import com.example.theloop.models.NewsResponse;
import com.example.theloop.models.WeatherResponse;
import com.example.theloop.network.FunFactApiService;
import com.example.theloop.network.NewsApiService;
import com.example.theloop.network.NewsRetrofitClient;
import com.example.theloop.network.RetrofitClient;
import com.example.theloop.network.WeatherApiService;
import com.example.theloop.utils.AppUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements DashboardAdapter.Binder, TextToSpeech.OnInitListener {

    private static final String TAG = "MainActivity";
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int HEALTH_PERMISSION_REQUEST_CODE = 102;

    static final String PREFS_NAME = "TheLoopPrefs";
    static final String KEY_FIRST_RUN = "is_first_run";
    static final String KEY_USER_NAME = "user_name";
    static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String WEATHER_CACHE_KEY = "weather_cache";
    private static final String NEWS_CACHE_KEY = "news_cache";
    private static final String KEY_SECTION_ORDER = "section_order";
    private static final String KEY_SUMMARY_CACHE = "summary_cache";
    static final String KEY_LATITUDE = "last_latitude";
    static final String KEY_LONGITUDE = "last_longitude";

    private static final String SECTION_HEADLINES = "headlines";
    private static final String SECTION_CALENDAR = "calendar";
    private static final String SECTION_FUN_FACT = "fun_fact";
    private static final String SECTION_HEALTH = "health";
    private static final String DEFAULT_SECTION_ORDER = SECTION_HEADLINES + "," + SECTION_CALENDAR + "," + SECTION_FUN_FACT + "," + SECTION_HEALTH;

    private static final double DEFAULT_LATITUDE = 51.5480;
    private static final double DEFAULT_LONGITUDE = -0.1030;

    private static final java.time.format.DateTimeFormatter WEATHER_DATE_INPUT_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    private static final java.time.format.DateTimeFormatter WEATHER_DATE_DAY_FORMAT = java.time.format.DateTimeFormatter.ofPattern("EEE d", Locale.getDefault());

    private Gson gson = new Gson();
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;
    private int selectedNewsCategory = R.id.chip_us;
    private NewsResponse cachedNewsResponse;
    private Runnable onLocationPermissionGranted;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private DashboardAdapter adapter;
    private RecyclerView recyclerView;
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private HealthConnectClient healthConnectClient;
    private final androidx.activity.result.ActivityResultLauncher<Set<String>> healthPermissionLauncher =
            registerForActivityResult(
                    androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
                    granted -> fetchHealthData());
    private String cachedLocationName;

    // Data State for Summary
    private WeatherResponse latestWeather;
    private List<CalendarEvent> latestEvents;
    private Article topHeadline;
    private String generatedSummary;

    // Data state for Fun Fact
    private String funFactText;

    // Data state for Health
    private long stepsToday = -1;
    private boolean healthPermissionDenied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (Geocoder.isPresent()) {
            geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(KEY_FIRST_RUN, true);

        initHealthConnect();

        // Init TTS
        textToSpeech = new TextToSpeech(this, this);

        if (isFirstRun) {
            runSetupSequence();
        } else {
            setupRecyclerView();
            refreshData();
        }

        // Schedule Widget Worker
        PeriodicWorkRequest widgetWorkRequest = new PeriodicWorkRequest.Builder(WidgetUpdateWorker.class, 30, java.util.concurrent.TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "widget_update",
                ExistingPeriodicWorkPolicy.KEEP,
                widgetWorkRequest);
    }

    private void initHealthConnect() {
         if (HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE) {
             healthConnectClient = HealthConnectClient.getOrCreate(this);
         }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.dashboard_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String order = prefs.getString(KEY_SECTION_ORDER, DEFAULT_SECTION_ORDER);
        String[] sections = order.split(",");

        adapter = new DashboardAdapter(this, sections);
        recyclerView.setAdapter(adapter);
    }

    private void refreshData() {
        fetchLocationAndThenWeatherData();
        // News fetch triggered by bindHeadlines logic or manually here?
        // Adapter binding triggers it usually, but we need data for summary too.
        // We can trigger background fetches here.
        fetchNewsDataForSummary();
        loadCalendarDataForSummary();
        fetchFunFact();
        fetchHealthData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    // --- TTS ---
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language not supported");
            } else {
                isTtsReady = true;
            }
        } else {
            Log.e(TAG, "TTS Initialization failed");
        }
    }

    private void speakSummary() {
        if (!isTtsReady || TextUtils.isEmpty(generatedSummary)) return;
        textToSpeech.speak(generatedSummary, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // --- DashboardBinder Implementation ---

    @Override
    public void bindHeader(DashboardAdapter.HeaderViewHolder holder) {
        holder.greeting.setText(getGreeting());
        if (generatedSummary != null) {
            holder.summary.setText(generatedSummary);
        } else {
            holder.summary.setText("Checking your day...");
        }

        holder.playButton.setOnClickListener(v -> speakSummary());
    }

    @Override
    public void bindWeather(DashboardAdapter.WeatherViewHolder holder) {
        holder.settingsIcon.setOnClickListener(v -> showTemperatureUnitDialog());

        if (latestWeather != null) {
            holder.progressBar.setVisibility(View.GONE);
            holder.contentLayout.setVisibility(View.VISIBLE);
            populateWeatherCard(holder, latestWeather);
            updateLocationName(holder);
        } else {
            // Check cache or show loading/error
             loadWeatherFromCache(holder);
        }
    }

    @Override
    public void bindHeadlines(DashboardAdapter.HeadlinesViewHolder holder) {
        holder.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) return;
            selectedNewsCategory = checkedId;
            if (cachedNewsResponse != null) {
                displayNewsForCategory(holder, cachedNewsResponse);
            } else {
                fetchNewsData(holder);
            }
        });

        if (cachedNewsResponse == null) {
            fetchNewsData(holder);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            displayNewsForCategory(holder, cachedNewsResponse);
        }
    }

    @Override
    public void bindCalendar(DashboardAdapter.CalendarViewHolder holder) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            holder.permissionDeniedText.setVisibility(View.VISIBLE);
            holder.eventsContainer.setVisibility(View.GONE);
            holder.permissionDeniedText.setOnClickListener(v ->
                 ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_REQUEST_CODE)
            );
        } else {
            holder.permissionDeniedText.setVisibility(View.GONE);
            if (latestEvents != null) {
                 populateCalendarCard(holder, latestEvents);
            } else {
                queryCalendarEvents(holder); // Fetch if not ready
            }
        }
    }

    @Override
    public void bindFunFact(DashboardAdapter.FunFactViewHolder holder) {
        if (funFactText != null) {
            holder.funFactText.setText(funFactText);
        } else {
            holder.funFactText.setText("Loading fun fact...");
        }
    }

    @Override
    public void bindHealth(DashboardAdapter.HealthViewHolder holder) {
        if (healthConnectClient == null) {
            holder.errorText.setText("Health Connect not available");
            holder.errorText.setVisibility(View.VISIBLE);
            holder.contentLayout.setVisibility(View.GONE);
            holder.permissionButton.setVisibility(View.GONE);
            return;
        }

        if (stepsToday >= 0) {
            holder.contentLayout.setVisibility(View.VISIBLE);
            holder.stepsCount.setText(String.valueOf(stepsToday));
            holder.permissionButton.setVisibility(View.GONE);
            holder.errorText.setVisibility(View.GONE);
        } else if (healthPermissionDenied) {
             holder.contentLayout.setVisibility(View.GONE);
             holder.permissionButton.setVisibility(View.VISIBLE);
             holder.permissionButton.setText("Permission Denied (Tap to Open Settings)");
             holder.permissionButton.setOnClickListener(v -> {
                 // Intent to open settings? Or just retry?
                 // Usually we can't re-request immediately if denied twice.
                 Toast.makeText(this, "Please enable permissions in Health Connect settings", Toast.LENGTH_LONG).show();
             });
        } else {
             holder.contentLayout.setVisibility(View.GONE);
             holder.permissionButton.setVisibility(View.VISIBLE);
             holder.permissionButton.setOnClickListener(v -> checkHealthPermissionsAndFetch());
        }
    }

    // --- Logic & Helpers ---

    private void runSetupSequence() {
        showNameDialog(this::onNameEntered);
    }

    private void onNameEntered() {
        requestLocationPermission(this::onLocationPermissionGrantedForSetup);
    }

    private void onLocationPermissionGrantedForSetup() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_FIRST_RUN, false).apply();
        setupRecyclerView();
        refreshData();
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

    // --- Weather Logic ---

    private void fetchLocationAndThenWeatherData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fetchWeatherData(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Save location for Widget
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putString(KEY_LATITUDE, String.valueOf(location.getLatitude()))
                                .putString(KEY_LONGITUDE, String.valueOf(location.getLongitude()))
                                .apply();

                        // Fetch location name
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1, addresses -> {
                                String city = "";
                                if (addresses != null && !addresses.isEmpty()) {
                                    city = addresses.get(0).getLocality();
                                    if (TextUtils.isEmpty(city)) city = addresses.get(0).getSubAdminArea();
                                }
                                cachedLocationName = TextUtils.isEmpty(city) ? getString(R.string.unknown_location) : city;
                            });
                        } else {
                            executorService.execute(() -> {
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    String city = "";
                                    if (addresses != null && !addresses.isEmpty()) {
                                        city = addresses.get(0).getLocality();
                                        if (TextUtils.isEmpty(city)) city = addresses.get(0).getSubAdminArea();
                                    }
                                    cachedLocationName = TextUtils.isEmpty(city) ? getString(R.string.unknown_location) : city;
                                } catch (Exception e) {}
                            });
                        }

                        fetchWeatherData(location.getLatitude(), location.getLongitude());
                    } else {
                        fetchWeatherData(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                    }
                })
                .addOnFailureListener(this, e -> {
                    fetchWeatherData(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                });
    }

    private void fetchWeatherData(double latitude, double longitude) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null || !cm.getActiveNetworkInfo().isConnected()) {
            Log.d(TAG, "No network connection, loading from cache.");
            adapter.notifyItemChanged(1); // Rebind to load from cache
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String unit = prefs.getString(KEY_TEMP_UNIT, getResources().getStringArray(R.array.temp_units_values)[0]);

        WeatherApiService apiService = RetrofitClient.getClient().create(WeatherApiService.class);
        Call<WeatherResponse> call = apiService.getWeather(latitude, longitude, "temperature_2m,weather_code", "weather_code,temperature_2m_max,temperature_2m_min", unit, "auto");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    latestWeather = response.body();
                    saveToCache(WEATHER_CACHE_KEY, latestWeather);
                    adapter.notifyItemChanged(1); // Update Weather card
                    refreshDailySummary();
                    updateWidget();
                }
            }
            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Weather failed", t);
            }
        });
    }

    private void loadWeatherFromCache(DashboardAdapter.WeatherViewHolder holder) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cachedJson = prefs.getString(WEATHER_CACHE_KEY, null);
        holder.progressBar.setVisibility(View.GONE);
        if (cachedJson != null) {
            latestWeather = gson.fromJson(cachedJson, WeatherResponse.class);
            holder.contentLayout.setVisibility(View.VISIBLE);
            populateWeatherCard(holder, latestWeather);
            updateLocationName(holder);
        } else {
            holder.errorText.setVisibility(View.VISIBLE);
        }
    }

    private void populateWeatherCard(DashboardAdapter.WeatherViewHolder holder, WeatherResponse weather) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String unit = prefs.getString(KEY_TEMP_UNIT, getResources().getStringArray(R.array.temp_units_values)[0]);
        String tempSymbol = unit.equals("celsius") ? "°C" : "°F";

        holder.temp.setText(String.format(Locale.getDefault(), "%.0f%s", weather.getCurrent().getTemperature(), tempSymbol));
        holder.conditions.setText(getString(AppUtils.getWeatherDescription(weather.getCurrent().getWeatherCode())));
        holder.icon.setImageResource(AppUtils.getWeatherIconResource(weather.getCurrent().getWeatherCode()));

        com.example.theloop.models.DailyWeather daily = weather.getDaily();
        if (daily != null && daily.getTime() != null) {
            holder.forecastContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);
            int minSize = Math.min(daily.getTime().size(),
                Math.min(daily.getTemperatureMax().size(), daily.getWeatherCode().size()));
            int daysToShow = Math.min(5, minSize);

             if (minSize > 0) {
                double maxTemp = daily.getTemperatureMax().get(0);
                double minTemp = daily.getTemperatureMin().get(0);
                holder.highLow.setText(String.format(Locale.getDefault(), "H:%.0f%s L:%.0f%s", maxTemp, tempSymbol, minTemp, tempSymbol));
            }

            for (int i = 0; i < daysToShow; i++) {
                View forecastView = inflater.inflate(R.layout.item_daily_forecast, holder.forecastContainer, false);
                TextView dayText = forecastView.findViewById(R.id.forecast_day);
                ImageView icon = forecastView.findViewById(R.id.forecast_icon);
                TextView high = forecastView.findViewById(R.id.forecast_high);
                TextView low = forecastView.findViewById(R.id.forecast_low);

                try {
                    java.time.LocalDate date = java.time.LocalDate.parse(daily.getTime().get(i), WEATHER_DATE_INPUT_FORMAT);
                    dayText.setText(date.format(WEATHER_DATE_DAY_FORMAT));
                } catch (Exception e) { dayText.setText("-"); }

                icon.setImageResource(AppUtils.getWeatherIconResource(daily.getWeatherCode().get(i)));
                high.setText(String.format(Locale.getDefault(), "%.0f%s", daily.getTemperatureMax().get(i), tempSymbol));
                low.setText(String.format(Locale.getDefault(), "%.0f%s", daily.getTemperatureMin().get(i), tempSymbol));

                holder.forecastContainer.addView(forecastView);
            }
        }
    }

    private void updateLocationName(DashboardAdapter.WeatherViewHolder holder) {
        if (holder == null || holder.location == null) return;
        if (cachedLocationName != null) {
            holder.location.setText(cachedLocationName);
        } else {
            holder.location.setText(R.string.unknown_location);
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
                refreshData();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    // --- News Logic ---

    private void fetchNewsData(DashboardAdapter.HeadlinesViewHolder holder) {
        if (holder != null) holder.progressBar.setVisibility(View.VISIBLE);
        NewsApiService apiService = NewsRetrofitClient.getClient().create(NewsApiService.class);
        apiService.getNewsFeed().enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedNewsResponse = response.body();
                    saveToCache(NEWS_CACHE_KEY, cachedNewsResponse);

                    // Extract top headline for summary (From US or world usually)
                    List<Article> defaults = cachedNewsResponse.getUs();
                    if (defaults != null && !defaults.isEmpty()) {
                        topHeadline = defaults.get(0);
                    }
                    refreshDailySummary();

                    if (holder != null) {
                        holder.progressBar.setVisibility(View.GONE);
                        displayNewsForCategory(holder, cachedNewsResponse);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                if (holder != null) holder.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchNewsDataForSummary() {
        // Just triggers the fetch if we don't have it, no UI holder needed
        fetchNewsData(null);
    }

    private void displayNewsForCategory(DashboardAdapter.HeadlinesViewHolder holder, NewsResponse response) {
        if (holder == null) return;
        List<Article> articles = switch (selectedNewsCategory) {
            case R.id.chip_business -> response.getBusiness();
            case R.id.chip_entertainment -> response.getEntertainment();
            case R.id.chip_health -> response.getHealth();
            case R.id.chip_science -> response.getScience();
            case R.id.chip_sports -> response.getSports();
            case R.id.chip_technology -> response.getTechnology();
            case R.id.chip_world -> response.getWorld();
            default -> response.getUs();
        };

        if (articles != null) {
            holder.container.removeAllViews();
            holder.errorText.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);
            int count = 0;
            for (Article article : articles) {
                if (count >= 3) break;
                View headlineView = inflater.inflate(R.layout.item_headline, holder.container, false);
                TextView title = headlineView.findViewById(R.id.headline_title);
                TextView sourceTextView = headlineView.findViewById(R.id.headline_source_time);
                title.setText(article.getTitle());
                sourceTextView.setText(article.getSource());
                headlineView.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl())));
                    } catch (Exception e) { Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show(); }
                });
                holder.container.addView(headlineView);
                count++;
            }
        } else {
            holder.errorText.setVisibility(View.VISIBLE);
        }
    }

    // --- Calendar Logic ---

    private void queryCalendarEvents(DashboardAdapter.CalendarViewHolder holder) {
         loadCalendarDataForSummary();
    }

    private void loadCalendarDataForSummary() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        executorService.execute(() -> {
            List<CalendarEvent> events = new ArrayList<>();
            try {
                ContentResolver contentResolver = getContentResolver();
                Uri uri = CalendarContract.Events.CONTENT_URI;
                long now = System.currentTimeMillis();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(now);
                cal.add(Calendar.HOUR_OF_DAY, 24);
                long end = cal.getTimeInMillis();

                String selection = CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTSTART + " <= ?";
                String[] selectionArgs = new String[]{String.valueOf(now), String.valueOf(end)};
                String sort = CalendarContract.Events.DTSTART + " ASC";

                try (Cursor cursor = contentResolver.query(uri, new String[]{
                        CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND, CalendarContract.Events.EVENT_LOCATION}, selection, selectionArgs, sort)) {
                    if (cursor != null) {
                         while (cursor.moveToNext() && events.size() < 3) {
                             events.add(new CalendarEvent(
                                 cursor.getLong(0), cursor.getString(1), cursor.getLong(2), cursor.getLong(3), cursor.getString(4)
                             ));
                         }
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Cal error", e); }

            latestEvents = events;
            runOnUiThread(() -> {
                adapter.notifyItemChanged(findPositionForSection(SECTION_CALENDAR));
                refreshDailySummary();
            });
        });
    }

    private void populateCalendarCard(DashboardAdapter.CalendarViewHolder holder, List<CalendarEvent> events) {
        holder.eventsContainer.removeAllViews();
        if (events.isEmpty()) {
            holder.noEventsText.setVisibility(View.VISIBLE);
            holder.eventsContainer.setVisibility(View.GONE);
        } else {
            holder.noEventsText.setVisibility(View.GONE);
            holder.eventsContainer.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (CalendarEvent event : events) {
                View view = inflater.inflate(R.layout.item_calendar_event, holder.eventsContainer, false);
                TextView title = view.findViewById(R.id.event_title);
                TextView time = view.findViewById(R.id.event_time);
                TextView loc = view.findViewById(R.id.event_location);
                title.setText(event.getTitle());
                time.setText(AppUtils.formatEventTime(this, event.getStartTime(), event.getEndTime()));
                if (!TextUtils.isEmpty(event.getLocation())) {
                    loc.setText(event.getLocation());
                    loc.setVisibility(View.VISIBLE);
                } else loc.setVisibility(View.GONE);
                holder.eventsContainer.addView(view);
            }
        }
    }

    // --- Fun Fact Logic ---

    private void fetchFunFact() {
        FunFactApiService api = FunFactRetrofitClient.getClient().create(FunFactApiService.class);
        api.getRandomFact().enqueue(new Callback<FunFactResponse>() {
            @Override
            public void onResponse(Call<FunFactResponse> call, Response<FunFactResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    funFactText = response.body().getText();
                } else {
                    loadFallbackFunFact();
                }
                adapter.notifyItemChanged(findPositionForSection(SECTION_FUN_FACT));
            }
            @Override
            public void onFailure(Call<FunFactResponse> call, Throwable t) {
                loadFallbackFunFact();
                adapter.notifyItemChanged(findPositionForSection(SECTION_FUN_FACT));
            }
        });
    }

    private void loadFallbackFunFact() {
        try {
            String[] facts = getResources().getStringArray(R.array.fun_facts);
            int idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % facts.length;
            funFactText = facts[idx];
        } catch (Exception e) { funFactText = "Did you know? Code is poetry."; }
    }

    // --- Health Connect Logic ---

    private void checkHealthPermissionsAndFetch() {
         Set<String> permissions = new HashSet<>();
         permissions.add(HealthPermission.getReadPermission(StepsRecord.class));
         healthPermissionLauncher.launch(permissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (onLocationPermissionGranted != null) {
                onLocationPermissionGranted.run();
            }
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndThenWeatherData();
            } else {
                Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            int calendarPosition = findPositionForSection(SECTION_CALENDAR);
            if (calendarPosition != -1) {
                adapter.notifyItemChanged(calendarPosition);
            }
        }
    }

    private void fetchHealthData() {
        if (healthConnectClient == null) return;

        new com.example.theloop.health.HealthConnectHelper(this).fetchStepsToday(new com.example.theloop.health.HealthConnectHelper.StepsCallback() {
            @Override
            public void onStepsFetched(long steps) {
                stepsToday = steps;
                adapter.notifyItemChanged(findPositionForSection(SECTION_HEALTH));
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Health error", e);
                // If permission error, maybe set flag
                // For now, keep stale or error state
                adapter.notifyItemChanged(findPositionForSection(SECTION_HEALTH));
            }
        });
    }

    // --- Dynamic Summary Logic ---

    private void refreshDailySummary() {
        if (latestWeather == null) return; // Wait for weather at least

        String userName = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_USER_NAME, "User");
        String condition = getString(AppUtils.getWeatherDescription(latestWeather.getCurrent().getWeatherCode()));
        double temp = latestWeather.getCurrent().getTemperature();

        int eventCount = (latestEvents != null) ? latestEvents.size() : 0;
        String nextEventTitle = (eventCount > 0) ? latestEvents.get(0).getTitle() : "no upcoming events";
        String newsTitle = (topHeadline != null) ? topHeadline.getTitle() : "No major news";

        // "Good morning [Name]. It is [Condition] and [Temp]. You have [Count] events, the next one is [Title]. Top news: [Headline]."
        String timeGreeting = getGreeting().split(",")[0];

        generatedSummary = String.format(Locale.getDefault(),
            "%s %s. It is %s and %.0f degrees. You have %d events%s. Top news: %s.",
            timeGreeting, userName, condition, temp, eventCount,
            (eventCount > 0 ? ", the next one is " + nextEventTitle : ""),
            newsTitle
        );

        // Update Adapter (Header)
        adapter.notifyItemChanged(0);

        // Update Widget Cache
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_SUMMARY_CACHE, generatedSummary).apply();
        updateWidget();
    }

    private void updateWidget() {
        Intent intent = new Intent(this, DayAheadWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), DayAheadWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    // --- Helpers ---

    private int findPositionForSection(String section) {
        String order = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_SECTION_ORDER, DEFAULT_SECTION_ORDER);
        String[] sections = order.split(",");
        for (int i=0; i<sections.length; i++) {
            if (sections[i].equals(section)) return i + 2; // +2 for Header and Weather
        }
        return -1;
    }

    private void saveToCache(String key, Object data) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(key, gson.toJson(data)).apply();
    }

    String getGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        if (timeOfDay >= 0 && timeOfDay < 12) return "Good morning";
        else if (timeOfDay >= 12 && timeOfDay < 17) return "Good afternoon";
        else return "Good evening";
    }
}
