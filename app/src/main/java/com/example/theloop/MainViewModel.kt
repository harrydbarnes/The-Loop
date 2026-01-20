package com.example.theloop

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theloop.data.repository.CalendarRepository
import com.example.theloop.data.repository.FunFactRepository
import com.example.theloop.data.repository.NewsRepository
import com.example.theloop.data.repository.WeatherRepository
import com.example.theloop.models.Article
import com.example.theloop.models.CalendarEvent
import com.example.theloop.models.WeatherResponse
import com.example.theloop.utils.AppConstants
import com.example.theloop.utils.SummaryUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val weather: WeatherResponse? = null,
    val newsArticles: List<Article> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val funFact: String? = null,
    val summary: String? = null,
    val locationName: String = "Loading...",
    val isLoading: Boolean = false,
    val userGreeting: String = "",
    val tempUnit: String = "celsius"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val weatherRepo: WeatherRepository,
    private val newsRepo: NewsRepository,
    private val calendarRepo: CalendarRepository,
    private val funFactRepo: FunFactRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _locationName = MutableStateFlow("Loading...")
    private val _funFact = MutableStateFlow<String?>(null)

    private val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    private val _tempUnit = MutableStateFlow(prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT) ?: "celsius")
    private val _userName = MutableStateFlow(prefs.getString(AppConstants.KEY_USER_NAME, "User") ?: "User")

    val weather = weatherRepo.weatherData
    // TODO: Re-implement category selection (regression). Hardcoded to "US" for now.
    val news = newsRepo.getArticles("US") // Default to US or General
    val events = calendarRepo.events

    val uiState: StateFlow<MainUiState> = combine(
        weather,
        news,
        events,
        _funFact,
        _locationName,
        _userName,
        _tempUnit
    ) { weatherData, newsData, calendarData, funFactData, locationName, userName, tempUnit ->

        val summaryText = if (weatherData != null) {
            SummaryUtils.generateSummary(
                context,
                weatherData,
                calendarData,
                calendarData.size,
                newsData.firstOrNull(),
                userName,
                false
            )
        } else null

        MainUiState(
            weather = weatherData,
            newsArticles = newsData,
            calendarEvents = calendarData,
            funFact = funFactData,
            summary = summaryText,
            locationName = locationName,
            userGreeting = "${SummaryUtils.getTimeBasedGreeting()}, $userName",
            tempUnit = tempUnit
        )
    }.onEach { state ->
        if (state.summary != null) {
            prefs.edit().putString(AppConstants.KEY_SUMMARY_CACHE, state.summary).apply()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        refreshAll()
    }

    fun refreshAll() {
        val latStr = prefs.getString(AppConstants.KEY_LATITUDE, AppConstants.DEFAULT_LATITUDE.toString())
        val lonStr = prefs.getString(AppConstants.KEY_LONGITUDE, AppConstants.DEFAULT_LONGITUDE.toString())

        val lat = try { latStr?.toDouble() ?: AppConstants.DEFAULT_LATITUDE } catch (e: Exception) { AppConstants.DEFAULT_LATITUDE }
        val lon = try { lonStr?.toDouble() ?: AppConstants.DEFAULT_LONGITUDE } catch (e: Exception) { AppConstants.DEFAULT_LONGITUDE }

        fetchWeather(lat, lon)
        fetchNews()
        fetchCalendar()
        fetchFunFact()
    }

    fun fetchWeather(lat: Double, lon: Double) {
         viewModelScope.launch {
             fetchLocationName(lat, lon)
             weatherRepo.refresh(lat, lon, _tempUnit.value)
         }
    }

    fun fetchNews() {
        viewModelScope.launch {
            newsRepo.refreshNews()
        }
    }

    fun fetchCalendar() {
        viewModelScope.launch {
            calendarRepo.refreshEvents()
        }
    }

    fun fetchFunFact() {
        viewModelScope.launch {
            val fact = funFactRepo.getFunFact()
            if (fact != null) {
                _funFact.value = fact
            } else {
                 _funFact.value = context.getString(R.string.fun_fact_fallback)
            }
        }
    }

    private fun fetchLocationName(lat: Double, lon: Double) {
        if (Geocoder.isPresent()) {
             viewModelScope.launch(Dispatchers.IO) {
                 try {
                     val geocoder = Geocoder(context, java.util.Locale.getDefault())
                     @Suppress("DEPRECATION")
                     val addresses = geocoder.getFromLocation(lat, lon, 1)
                     if (!addresses.isNullOrEmpty()) {
                         val address = addresses[0]
                         val name = address.locality ?: address.subAdminArea ?: "Unknown Location"
                         _locationName.value = name
                     }
                } catch (e: Exception) {
                    _locationName.value = "Unknown Location"
                }
             }
        }
    }
}
