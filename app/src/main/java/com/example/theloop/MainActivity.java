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
import androidx.lifecycle.ViewModelProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.theloop.models.Article;
import com.example.theloop.models.CalendarEvent;
import com.example.theloop.models.NewsResponse;
import com.example.theloop.models.WeatherResponse;
import com.example.theloop.utils.AppConstants;
import com.example.theloop.utils.AppUtils;
import com.example.theloop.health.HealthConnectHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.jvm.JvmClassMappingKt;

public class MainActivity extends AppCompatActivity implements DashboardAdapter.Binder, TextToSpeech.OnInitListener {

    private static final String TAG = "MainActivity";
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 100;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int HEALTH_PERMISSION_REQUEST_CODE = 102;

    public static final String SECTION_HEADLINES = "headlines";
    public static final String SECTION_UK_NEWS = "uk_news";
    public static final String SECTION_CALENDAR = "calendar";
    public static final String SECTION_FUN_FACT = "fun_fact";
    public static final String SECTION_HEALTH = "health";

    private static final int POSITION_HEADER = 0;
    private static final int POSITION_WEATHER = 1;

    // UK News moved to front (after weather, before headlines)
    private static final String DEFAULT_SECTION_ORDER = SECTION_UK_NEWS + "," + SECTION_HEADLINES + "," + SECTION_CALENDAR + "," + SECTION_FUN_FACT + "," + SECTION_HEALTH;

    private static final String[] CALENDAR_PROJECTION = new String[]{
            CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND, CalendarContract.Events.EVENT_LOCATION
    };

    private MainViewModel viewModel;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int selectedNewsCategory = R.id.chip_us;
    private NewsResponse cachedNewsResponse;
    private Runnable onLocationPermissionGranted;
    private DashboardAdapter adapter;
    private RecyclerView recyclerView;
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private boolean weatherError = false;
    private boolean newsError = false;
    private boolean isFetchingNews = false;
    private HealthConnectClient healthConnectClient;
    private HealthConnectHelper healthConnectHelper;
    private final androidx.activity.result.ActivityResultLauncher<Set<String>> healthPermissionLauncher =
            registerForActivityResult(
                    androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
                    granted -> {
                        if (granted.contains(HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(StepsRecord.class)))) {
                            fetchHealthData();
                        } else {
                            healthPermissionDenied = true;
                            if (adapter != null) {
                                adapter.notifyItemChanged(findPositionForSection(SECTION_HEALTH));
                            }
                        }
                    });

    private final androidx.activity.result.ActivityResultLauncher<String> requestCalendarPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    viewModel.loadCalendarData();
                } else {
                    int calendarPosition = findPositionForSection(SECTION_CALENDAR);
                    if (calendarPosition != -1) {
                        adapter.notifyItemChanged(calendarPosition);
                    }
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (onLocationPermissionGranted != null) {
                    onLocationPermissionGranted.run();
                    onLocationPermissionGranted = null;
                }

                if (isGranted) {
                    fetchLocationAndThenWeatherData();
                } else {
                    Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show();
                }
            });

    private String cachedLocationName;
    private WeatherResponse latestWeather;
    private List<String> cachedForecastDates;
    private List<CalendarEvent> latestEvents;
    private int totalEventCount = 0;
    private boolean calendarQueryError = false;
    private String generatedSummary;
    private String funFactText;
    private long stepsToday = -1;
    private boolean healthPermissionDenied = false;

    // Cache fields for performance
    private Map<String, Integer> sectionPositions;
    private String currentTempUnit;
    private String currentUserName;
    private List<String> ukSources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        boolean onboardingCompleted = prefs.getBoolean(AppConstants.KEY_ONBOARDING_COMPLETED, false);

        if (!onboardingCompleted) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        // Initialize cached values
        currentTempUnit = prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT);
        currentUserName = prefs.getString(AppConstants.KEY_USER_NAME, "");
        ukSources = Arrays.asList(getResources().getStringArray(R.array.uk_news_sources));

        initHealthConnect();
        textToSpeech = new TextToSpeech(this, this);

        observeViewModel();

        viewModel.getLocationName().observe(this, name -> {
            cachedLocationName = name;
            if (adapter != null) adapter.notifyItemChanged(POSITION_WEATHER);
        });

        setupRecyclerView();
        refreshData();

        androidx.work.Constraints constraints = new androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest widgetWorkRequest = new PeriodicWorkRequest.Builder(WidgetUpdateWorker.class, 30, java.util.concurrent.TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "widget_update",
                ExistingPeriodicWorkPolicy.KEEP,
                widgetWorkRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh preferences that might have changed in SettingsActivity
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        String newUnit = prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT);
        if (!newUnit.equals(currentTempUnit)) {
            currentTempUnit = newUnit;
            if (latestWeather != null) {
                // Trigger re-bind
                if (adapter != null) adapter.notifyItemChanged(POSITION_WEATHER);
                // Also re-fetch to get correct unit data from API if needed,
                // but API call is needed only if we want server-side conversion or if we just convert locally.
                // Current implementation fetches with unit parameter.
                fetchLocationAndThenWeatherData();
            }
        }
    }

    private void observeViewModel() {
        viewModel.getLatestWeather().observe(this, weather -> {
            latestWeather = weather;
            if (weather != null && weather.getDaily() != null) {
                cachedForecastDates = AppUtils.formatForecastDates(weather.getDaily().getTime());
            } else {
                cachedForecastDates = null;
            }
            if (adapter != null) adapter.notifyItemChanged(POSITION_WEATHER);
        });

        viewModel.getCachedNewsResponse().observe(this, news -> {
            isFetchingNews = false;
            cachedNewsResponse = news;
            if (adapter != null) {
                adapter.notifyItemChanged(findPositionForSection(SECTION_HEADLINES));
                adapter.notifyItemChanged(findPositionForSection(SECTION_UK_NEWS));
            }
        });

        viewModel.getFunFactText().observe(this, fact -> {
            funFactText = fact;
            if (adapter != null) {
                adapter.notifyItemChanged(findPositionForSection(SECTION_FUN_FACT));
            }
        });

        viewModel.getCalendarEvents().observe(this, events -> {
            latestEvents = events;
            if (adapter != null) {
                adapter.notifyItemChanged(findPositionForSection(SECTION_CALENDAR));
            }
        });

        viewModel.getTotalEventCount().observe(this, count -> {
            totalEventCount = count;
        });

        viewModel.getCalendarQueryError().observe(this, isError -> {
            calendarQueryError = isError;
            if (adapter != null) {
                adapter.notifyItemChanged(findPositionForSection(SECTION_CALENDAR));
            }
        });

        viewModel.getSummary().observe(this, summary -> {
            generatedSummary = summary;
            if (adapter != null) adapter.notifyItemChanged(POSITION_HEADER);
            updateWidget();
        });

        viewModel.getWeatherError().observe(this, error -> {
            weatherError = error;
            if (error && adapter != null) adapter.notifyItemChanged(POSITION_WEATHER);
        });

        viewModel.getNewsError().observe(this, error -> {
            isFetchingNews = false;
            newsError = error;
            if (error && adapter != null) {
                adapter.notifyItemChanged(findPositionForSection(SECTION_HEADLINES));
                adapter.notifyItemChanged(findPositionForSection(SECTION_UK_NEWS));
            }
        });
    }

    private void initHealthConnect() {
         if (HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE) {
             healthConnectClient = HealthConnectClient.getOrCreate(this);
             healthConnectHelper = new HealthConnectHelper(this);
         }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.dashboard_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        String order = prefs.getString(AppConstants.KEY_SECTION_ORDER, DEFAULT_SECTION_ORDER);

        // Ensure UK News is present for existing users
        if (!order.contains(SECTION_UK_NEWS)) {
            order = SECTION_UK_NEWS + "," + order;
            prefs.edit().putString(AppConstants.KEY_SECTION_ORDER, order).apply();
        }

        String[] sections = order.split(",");

        // OPTIMIZATION: Cache positions to avoid repeated array searches and string splitting
        sectionPositions = new HashMap<>();
        for (int i = 0; i < sections.length; i++) {
            sectionPositions.put(sections[i], i + 2); // +2 for Header and Weather
        }

        adapter = new DashboardAdapter(this, sections);
        recyclerView.setAdapter(adapter);
    }

    private void refreshData() {
        fetchLocationAndThenWeatherData();
        fetchNewsDataForSummary();
        loadCalendarDataForSummary();
        fetchFunFact();
        fetchHealthData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (healthConnectHelper != null) {
            healthConnectHelper.cancel();
        }
    }

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

    @Override
    public void bindHeader(DashboardAdapter.HeaderViewHolder holder) {
        holder.greeting.setText(getGreeting());
        if (generatedSummary != null) {
            holder.summary.setText(generatedSummary);
        } else {
            holder.summary.setText(getString(R.string.checking_your_day));
        }
        holder.playButton.setOnClickListener(v -> speakSummary());
    }

    @Override
    public void bindWeather(DashboardAdapter.WeatherViewHolder holder) {
        holder.settingsIcon.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        if (latestWeather != null) {
            holder.progressBar.setVisibility(View.GONE);
            holder.errorText.setVisibility(View.GONE);
            holder.contentLayout.setVisibility(View.VISIBLE);
            populateWeatherCard(holder, latestWeather);
            updateLocationName(holder);
        } else if (weatherError) {
            holder.progressBar.setVisibility(View.GONE);
            holder.contentLayout.setVisibility(View.GONE);
            holder.errorText.setVisibility(View.VISIBLE);
        } else {
             loadWeatherFromCache(holder);
        }
    }

    @Override
    public void bindHeadlines(DashboardAdapter.HeadlinesViewHolder holder) {
        if (holder.cardTitle != null) holder.cardTitle.setText("Top Headlines");
        holder.chipGroup.setVisibility(View.VISIBLE);
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
            if (newsError) {
                holder.progressBar.setVisibility(View.GONE);
                holder.errorText.setVisibility(View.VISIBLE);
            } else {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.errorText.setVisibility(View.GONE);
                fetchNewsData(holder);
            }
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.errorText.setVisibility(View.GONE);
            displayNewsForCategory(holder, cachedNewsResponse);
        }
    }

    @Override
    public void bindUkNews(DashboardAdapter.HeadlinesViewHolder holder) {
        if (holder.cardTitle != null) holder.cardTitle.setText("UK News");
        holder.chipGroup.setVisibility(View.GONE);

        if (cachedNewsResponse == null) {
            if (newsError) {
                holder.progressBar.setVisibility(View.GONE);
                holder.errorText.setVisibility(View.VISIBLE);
            } else {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.errorText.setVisibility(View.GONE);
                // Re-use fetch logic
                fetchNewsData(null); // Just trigger fetch
            }
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.errorText.setVisibility(View.GONE);
            displayUkNews(holder, cachedNewsResponse);
        }
    }

    @Override
    public void bindCalendar(DashboardAdapter.CalendarViewHolder holder) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            holder.permissionDeniedText.setVisibility(View.VISIBLE);
            holder.eventsContainer.setVisibility(View.GONE);
            holder.errorText.setVisibility(View.GONE);
            holder.noEventsText.setVisibility(View.GONE);
            holder.permissionDeniedText.setOnClickListener(v ->
                 requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            );
        } else {
            holder.permissionDeniedText.setVisibility(View.GONE);
            if (calendarQueryError) {
                holder.errorText.setVisibility(View.VISIBLE);
                holder.eventsContainer.setVisibility(View.GONE);
                holder.noEventsText.setVisibility(View.GONE);
            } else if (latestEvents != null) {
                 populateCalendarCard(holder, latestEvents);
            } else {
                loadCalendarDataForSummary();
            }
        }
    }

    @Override
    public void bindFunFact(DashboardAdapter.FunFactViewHolder holder) {
        if (funFactText != null) {
            holder.funFactText.setText(funFactText);
        } else {
            holder.funFactText.setText(getString(R.string.loading_fun_fact));
        }
    }

    @Override
    public void bindHealth(DashboardAdapter.HealthViewHolder holder) {
        if (healthConnectClient == null) {
            holder.errorText.setText(getString(R.string.health_connect_not_available));
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
             holder.permissionButton.setText(getString(R.string.health_permission_denied_button));
             holder.permissionButton.setOnClickListener(v -> {
                 Intent intent = new Intent("androidx.health.connect.client.action.HEALTH_CONNECT_SETTINGS");
                 try {
                     startActivity(intent);
                 } catch (Exception e) {
                     // Try opening the Health Connect app on Play Store if not found
                     try {
                         startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")));
                     } catch (Exception ex) {
                        Toast.makeText(this, getString(R.string.health_settings_error), Toast.LENGTH_LONG).show();
                     }
                 }
             });
        } else {
             holder.contentLayout.setVisibility(View.GONE);
             holder.permissionButton.setVisibility(View.VISIBLE);
             holder.permissionButton.setOnClickListener(v -> checkHealthPermissionsAndFetch());
        }
    }

    @Override
    public void bindFooter(DashboardAdapter.FooterViewHolder holder) {
        holder.settingsLink.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }


    private void fetchLocationAndThenWeatherData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fetchWeatherData(AppConstants.DEFAULT_LATITUDE, AppConstants.DEFAULT_LONGITUDE);
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE).edit()
                                .putString(AppConstants.KEY_LATITUDE, String.valueOf(location.getLatitude()))
                                .putString(AppConstants.KEY_LONGITUDE, String.valueOf(location.getLongitude()))
                                .apply();

                        if (Geocoder.isPresent()) {
                             viewModel.fetchLocationName(location);
                        } else {
                             cachedLocationName = getString(R.string.unknown_location);
                             if (adapter != null) adapter.notifyItemChanged(POSITION_WEATHER);
                        }

                        fetchWeatherData(location.getLatitude(), location.getLongitude());
                    } else {
                        fetchWeatherData(AppConstants.DEFAULT_LATITUDE, AppConstants.DEFAULT_LONGITUDE);
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Failed to get location.", e);
                    fetchWeatherData(AppConstants.DEFAULT_LATITUDE, AppConstants.DEFAULT_LONGITUDE);
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void fetchWeatherData(double latitude, double longitude) {
        if (!isNetworkAvailable()) {
            viewModel.loadWeatherFromCache();
            return;
        }
        viewModel.fetchWeatherData(latitude, longitude);
    }

    private void loadWeatherFromCache(DashboardAdapter.WeatherViewHolder holder) {
        if (latestWeather == null) {
             viewModel.loadWeatherFromCache();
        }
    }

    private void populateWeatherCard(DashboardAdapter.WeatherViewHolder holder, WeatherResponse weather) {
        String unit = currentTempUnit;
        String tempSymbol = unit.equals("celsius") ? "°C" : "°F";

        // OPTIMIZATION: Use Math.round() and concatenation instead of String.format() for performance
        holder.temp.setText(Math.round(weather.getCurrent().getTemperature()) + tempSymbol);
        holder.conditions.setText(getString(AppUtils.getWeatherDescription(weather.getCurrent().getWeatherCode())));
        holder.icon.setImageResource(AppUtils.getWeatherIconResource(weather.getCurrent().getWeatherCode()));

        com.example.theloop.models.DailyWeather daily = weather.getDaily();
        if (daily != null && daily.getTime() != null) {
            int minSize = Math.min(daily.getTime().size(),
                Math.min(daily.getTemperatureMax().size(), daily.getWeatherCode().size()));
            int daysToShow = Math.min(holder.forecastViews.length, minSize);

             if (minSize > 0) {
                double maxTemp = daily.getTemperatureMax().get(0);
                double minTemp = daily.getTemperatureMin().get(0);
                holder.highLow.setText(getString(R.string.weather_high_prefix) + Math.round(maxTemp) + tempSymbol + " " + getString(R.string.weather_low_prefix) + Math.round(minTemp) + tempSymbol);
            }

            for (int i = 0; i < holder.forecastViews.length; i++) {
                DashboardAdapter.WeatherViewHolder.ForecastDayViewHolder dailyHolder = holder.forecastViews[i];
                if (i < daysToShow) {
                    dailyHolder.parent.setVisibility(View.VISIBLE);
                    TextView dayText = dailyHolder.day;
                    ImageView icon = dailyHolder.icon;
                    TextView high = dailyHolder.high;
                    TextView low = dailyHolder.low;

                    if (cachedForecastDates != null && i < cachedForecastDates.size()) {
                        dayText.setText(cachedForecastDates.get(i));
                    } else {
                        dayText.setText("-");
                    }

                    icon.setImageResource(AppUtils.getWeatherIconResource(daily.getWeatherCode().get(i)));
                    high.setText(Math.round(daily.getTemperatureMax().get(i)) + tempSymbol);
                    low.setText(Math.round(daily.getTemperatureMin().get(i)) + tempSymbol);
                } else {
                    dailyHolder.parent.setVisibility(View.GONE);
                }
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


    private void fetchNewsData(DashboardAdapter.HeadlinesViewHolder holder) {
        if (isFetchingNews) return;
        isFetchingNews = true;
        if (holder != null) holder.progressBar.setVisibility(View.VISIBLE);
        viewModel.fetchNewsData();
    }

    private void fetchNewsDataForSummary() {
        viewModel.fetchNewsData();
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
        populateHeadlines(holder, articles);
    }

    private void displayUkNews(DashboardAdapter.HeadlinesViewHolder holder, NewsResponse response) {
        if (holder == null) return;

        List<Article> allArticles = new ArrayList<>();
        if (response.getWorld() != null) allArticles.addAll(response.getWorld());
        if (response.getBusiness() != null) allArticles.addAll(response.getBusiness());
        if (response.getSports() != null) allArticles.addAll(response.getSports());

        List<Article> ukArticles = new ArrayList<>();
        for (Article a : allArticles) {
            for (String source : ukSources) {
                if (a.getSource() != null && a.getSource().contains(source)) {
                    ukArticles.add(a);
                    break;
                }
            }
        }

        if (ukArticles.isEmpty() && response.getWorld() != null) {
            ukArticles = response.getWorld();
        }

        populateHeadlines(holder, ukArticles);
    }

    private void populateHeadlines(DashboardAdapter.HeadlinesViewHolder holder, List<Article> articles) {
        if (articles != null && !articles.isEmpty()) {
            holder.errorText.setVisibility(View.GONE);
            for (int i = 0; i < holder.headlineViews.length; i++) {
                DashboardAdapter.HeadlinesViewHolder.HeadlineItemViewHolder itemHolder = holder.headlineViews[i];
                if (i < articles.size()) {
                    Article article = articles.get(i);
                    itemHolder.parent.setVisibility(View.VISIBLE);
                    TextView title = itemHolder.title;
                    TextView sourceTextView = itemHolder.source;
                    title.setText(article.getTitle());
                    sourceTextView.setText(article.getSource());
                    itemHolder.parent.setOnClickListener(v -> {
                        if (article.getUrl() != null) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl())));
                            } catch (android.content.ActivityNotFoundException e) {
                                Toast.makeText(this, "No browser found to open link.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Failed to open article link", e);
                            }
                        } else {
                            Toast.makeText(this, "Article link unavailable.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    itemHolder.parent.setVisibility(View.GONE);
                }
            }
        } else {
            holder.errorText.setVisibility(View.VISIBLE);
            // Hide all items
             for (DashboardAdapter.HeadlinesViewHolder.HeadlineItemViewHolder itemHolder : holder.headlineViews) {
                 itemHolder.parent.setVisibility(View.GONE);
             }
        }
    }

    private void loadCalendarDataForSummary() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        viewModel.loadCalendarData();
    }

    private void populateCalendarCard(DashboardAdapter.CalendarViewHolder holder, List<CalendarEvent> events) {
        holder.errorText.setVisibility(View.GONE);
        if (events.isEmpty()) {
            holder.noEventsText.setVisibility(View.VISIBLE);
            holder.eventsContainer.setVisibility(View.GONE);
        } else {
            holder.noEventsText.setVisibility(View.GONE);
            holder.eventsContainer.setVisibility(View.VISIBLE);
            for (int i = 0; i < holder.eventViews.length; i++) {
                DashboardAdapter.CalendarViewHolder.CalendarEventItemViewHolder itemHolder = holder.eventViews[i];
                if (i < events.size()) {
                    CalendarEvent event = events.get(i);
                    itemHolder.parent.setVisibility(View.VISIBLE);
                    TextView title = itemHolder.title;
                    TextView time = itemHolder.time;
                    TextView loc = itemHolder.location;
                    TextView owner = itemHolder.owner;

                    title.setText(event.getTitle());
                    time.setText(AppUtils.formatEventTime(this, event.getStartTime(), event.getEndTime()));

                    if (!TextUtils.isEmpty(event.getLocation())) {
                        loc.setText(event.getLocation());
                        loc.setVisibility(View.VISIBLE);
                    } else loc.setVisibility(View.GONE);

                    if (!TextUtils.isEmpty(event.getOwnerName())) {
                         owner.setText(event.getOwnerName());
                         owner.setVisibility(View.VISIBLE);
                    } else {
                         owner.setVisibility(View.GONE);
                    }

                    itemHolder.parent.setOnClickListener(v -> {
                        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.getId());
                        Intent intent = new Intent(Intent.ACTION_VIEW).setData(uri);
                        try {
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException e) {
                            Log.e(TAG, "Cannot open calendar event", e);
                            Toast.makeText(MainActivity.this, "No app found to open calendar event.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    itemHolder.parent.setVisibility(View.GONE);
                }
            }
        }
    }

    private void fetchFunFact() {
        viewModel.fetchFunFact();
    }

    private void checkHealthPermissionsAndFetch() {
         Set<String> permissions = new HashSet<>();
         permissions.add(HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(StepsRecord.class)));
         healthPermissionLauncher.launch(permissions);
    }

    private void fetchHealthData() {
        if (healthConnectClient == null || healthConnectHelper == null) return;

        healthConnectHelper.fetchStepsToday(new HealthConnectHelper.StepsCallback() {
            @Override
            public void onStepsFetched(long steps) {
                stepsToday = steps;
                adapter.notifyItemChanged(findPositionForSection(SECTION_HEALTH));
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Health error", e);
                adapter.notifyItemChanged(findPositionForSection(SECTION_HEALTH));
            }
        });
    }

    private void updateWidget() {
        Intent intent = new Intent(this, DayAheadWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), DayAheadWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private int findPositionForSection(String section) {
        if (sectionPositions != null) {
            Integer position = sectionPositions.get(section);
            if (position != null) {
                return position;
            }
        }

        // Fallback for edge cases (e.g. before setupRecyclerView or if map missing)
        String order = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE).getString(AppConstants.KEY_SECTION_ORDER, DEFAULT_SECTION_ORDER);
        String[] sections = order.split(",");
        for (int i=0; i<sections.length; i++) {
            if (sections[i].equals(section)) return i + 2;
        }
        return -1;
    }

    private String getTimeBasedGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        if (timeOfDay >= 0 && timeOfDay < 12) return "Good morning";
        else if (timeOfDay >= 12 && timeOfDay < 17) return "Good afternoon";
        else return "Good evening";
    }

    String getGreeting() {
        String userName = currentUserName;
        String greeting = getTimeBasedGreeting();

        if (!TextUtils.isEmpty(userName)) {
            return greeting + ", " + userName;
        }
        return greeting;
    }
}
