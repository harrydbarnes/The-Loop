package com.example.theloop

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theloop.data.repository.CalendarRepository
import com.example.theloop.data.repository.FunFactRepository
import com.example.theloop.data.repository.NewsRepository
import com.example.theloop.data.repository.UserPreferencesRepository
import com.example.theloop.data.repository.WeatherRepository
import com.example.theloop.models.Article
import com.example.theloop.models.CalendarEvent
import com.example.theloop.models.WeatherResponse
import com.example.theloop.utils.SummaryUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

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
    private val userPrefsRepo: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _locationName = MutableStateFlow("Loading...")
    private val _funFact = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _newsCategory = MutableStateFlow("US")

    val weather = weatherRepo.weatherData
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val news = _newsCategory.flatMapLatest { category ->
        newsRepo.getArticles(category)
    }
    val events = calendarRepo.events

    val uiState: StateFlow<MainUiState> = combine(
        weather, news, events, _funFact, _locationName
    ) { weatherData, newsData, calendarData, funFactData, locationName ->
        MainUiState(
            weather = weatherData,
            newsArticles = newsData,
            calendarEvents = calendarData,
            funFact = funFactData,
            locationName = locationName
        )
    }.combine(
        combine(userPrefsRepo.userName, userPrefsRepo.tempUnit, _isLoading) { userName, tempUnit, isLoading ->
            Triple(userName, tempUnit, isLoading)
        }
    ) { state, (userName, tempUnit, isLoading) ->
        val summaryText = if (state.weather != null) {
            SummaryUtils.generateSummary(
                context,
                state.weather,
                state.calendarEvents,
                state.calendarEvents.size,
                state.newsArticles.firstOrNull(),
                userName,
                false
            )
        } else null

        state.copy(
            summary = summaryText,
            userGreeting = "${SummaryUtils.getTimeBasedGreeting()}, $userName",
            tempUnit = tempUnit,
            isLoading = isLoading
        )
    }.onEach { state ->
        if (state.summary != null) {
            userPrefsRepo.saveSummary(state.summary)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            val (lat, lon) = userPrefsRepo.location.first()

            val jobs = listOf(
                launch { fetchWeather(lat, lon) },
                launch { fetchNews() },
                launch { fetchCalendar() },
                launch { fetchFunFact() }
            )
            jobs.joinAll()
            _isLoading.value = false
        }
    }

    fun updateLocation(lat: Double, lon: Double) {
        userPrefsRepo.updateLocation(lat, lon)
        refreshAll()
    }

    fun setNewsCategory(category: String) {
        _newsCategory.value = category
    }

    suspend fun fetchWeather(lat: Double, lon: Double) {
        try {
            // Bolt: Parallelize independent IO operations to reduce total latency.
            // fetchLocationName uses Geocoder (IO/IPC), weatherRepo.refresh uses Network.
            coroutineScope {
                launch { fetchLocationName(lat, lon) }
                launch { weatherRepo.refresh(lat, lon, userPrefsRepo.tempUnit.first()) }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error fetching weather or location name", e)
        }
    }

    suspend fun fetchNews() {
        newsRepo.refreshNews()
    }

    suspend fun fetchCalendar() {
        calendarRepo.refreshEvents()
    }

    suspend fun fetchFunFact() {
        val fact = funFactRepo.getFunFact()
        if (fact != null) {
            _funFact.value = fact
        } else {
             _funFact.value = context.getString(R.string.fun_fact_fallback)
        }
    }

    private suspend fun fetchLocationName(lat: Double, lon: Double) {
        if (Geocoder.isPresent()) {
             withContext(Dispatchers.IO) {
                 try {
                     val geocoder = Geocoder(context, java.util.Locale.getDefault())
                     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                         suspendCancellableCoroutine { cont ->
                             val listener = object : Geocoder.GeocodeListener {
                                 override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                     if (addresses.isNotEmpty()) {
                                         val address = addresses[0]
                                         val name = address.locality ?: address.subAdminArea ?: "Unknown Location"
                                         _locationName.value = name
                                     }
                                     if (cont.isActive) cont.resume(Unit)
                                 }

                                 override fun onError(errorMessage: String?) {
                                     if (cont.isActive) cont.resume(Unit)
                                 }
                             }
                             geocoder.getFromLocation(lat, lon, 1, listener)
                         }
                     } else {
                         @Suppress("DEPRECATION")
                         val addresses = geocoder.getFromLocation(lat, lon, 1)
                         if (!addresses.isNullOrEmpty()) {
                             val address = addresses[0]
                             val name = address.locality ?: address.subAdminArea ?: "Unknown Location"
                             _locationName.value = name
                         }
                     }
                } catch (e: Exception) {
                    _locationName.value = "Unknown Location"
                }
             }
        }
    }
}
