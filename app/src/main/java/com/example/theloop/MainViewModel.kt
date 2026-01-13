package com.example.theloop

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.theloop.models.CalendarEvent
import com.example.theloop.models.FunFactResponse
import com.example.theloop.models.NewsResponse
import com.example.theloop.models.WeatherResponse
import com.example.theloop.network.FunFactApiService
import com.example.theloop.network.FunFactRetrofitClient
import com.example.theloop.network.NewsApiService
import com.example.theloop.network.NewsRetrofitClient
import com.example.theloop.network.RetrofitClient
import com.example.theloop.network.WeatherApiService
import com.example.theloop.utils.AppConstants
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"
    private val gson = Gson()

    private val _latestWeather = MutableLiveData<WeatherResponse>()
    val latestWeather: LiveData<WeatherResponse> = _latestWeather

    private val _cachedNewsResponse = MutableLiveData<NewsResponse>()
    val cachedNewsResponse: LiveData<NewsResponse> = _cachedNewsResponse

    private val _funFactText = MutableLiveData<String>()
    val funFactText: LiveData<String> = _funFactText

    private val _calendarEvents = MutableLiveData<List<CalendarEvent>>()
    val calendarEvents: LiveData<List<CalendarEvent>> = _calendarEvents

    private val _totalEventCount = MutableLiveData(0)
    val totalEventCount: LiveData<Int> = _totalEventCount

    private val _calendarQueryError = MutableLiveData(false)
    val calendarQueryError: LiveData<Boolean> = _calendarQueryError

    private val _locationName = MutableLiveData<String>()
    val locationName: LiveData<String> = _locationName

    private val _summary = androidx.lifecycle.MediatorLiveData<String>()
    val summary: LiveData<String> = _summary

    private val CALENDAR_PROJECTION = arrayOf(
        CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND, CalendarContract.Events.EVENT_LOCATION, CalendarContract.Events.CALENDAR_DISPLAY_NAME
    )

    fun fetchLocationName(location: Location) {
        val geocoder = Geocoder(getApplication(), java.util.Locale.getDefault())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                processGeocoderAddresses(addresses)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    processGeocoderAddresses(addresses)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get location name from geocoder", e)
                    processGeocoderAddresses(null)
                }
            }
        }
    }

    private fun processGeocoderAddresses(addresses: List<Address>?) {
        val unknown = getApplication<Application>().getString(R.string.unknown_location)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val city = address.locality
            val district = address.subAdminArea
            val sb = StringBuilder()
            if (!city.isNullOrEmpty()) sb.append(city)
            else if (!district.isNullOrEmpty()) sb.append(district)
            else sb.append(unknown)
            _locationName.postValue(sb.toString())
        } else {
            _locationName.postValue(unknown)
        }
    }

    init {
        val updateSummary = {
            val weather = _latestWeather.value
            val events = _calendarEvents.value
            val totalEvents = _totalEventCount.value ?: 0
            val news = _cachedNewsResponse.value
            val calendarError = _calendarQueryError.value ?: false
            val userName = getApplication<Application>().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(AppConstants.KEY_USER_NAME, "User") ?: "User"

            val topHeadline = news?.us?.firstOrNull()

            if (weather != null) {
                val summaryText = com.example.theloop.utils.SummaryUtils.generateSummary(
                    getApplication(),
                    weather,
                    events,
                    totalEvents,
                    topHeadline,
                    userName,
                    calendarError
                )
                _summary.postValue(summaryText)
                saveSummaryToCache(summaryText)
            }
        }

        _summary.addSource(_latestWeather) { updateSummary() }
        _summary.addSource(_calendarEvents) { updateSummary() }
        _summary.addSource(_cachedNewsResponse) { updateSummary() }
        _summary.addSource(_totalEventCount) { updateSummary() }
    }

    fun fetchWeatherData(latitude: Double, longitude: Double) {
        val prefs = getApplication<Application>().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val unit = prefs.getString(AppConstants.KEY_TEMP_UNIT, null) ?: getApplication<Application>().resources.getStringArray(R.array.temp_units_values)[0]

        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.getClient().create(WeatherApiService::class.java)
                val response = apiService.getWeather(latitude, longitude, "temperature_2m,weather_code", "weather_code,temperature_2m_max,temperature_2m_min", unit, "auto")
                if (response.isSuccessful && response.body() != null) {
                    _latestWeather.postValue(response.body())
                    saveToCache(AppConstants.WEATHER_CACHE_KEY, response.body())
                } else {
                    Log.e(TAG, "Weather API response not successful: " + response.code())
                    loadWeatherFromCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Weather failed", e)
                loadWeatherFromCache()
            }
        }
    }

    fun loadWeatherFromCache() {
        val prefs = getApplication<Application>().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(AppConstants.WEATHER_CACHE_KEY, null)
        if (cachedJson != null) {
            try {
                val weather = gson.fromJson(cachedJson, WeatherResponse::class.java)
                _latestWeather.postValue(weather)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load weather from cache", e)
            }
        }
    }

    fun fetchNewsData() {
        viewModelScope.launch {
            try {
                val apiService = NewsRetrofitClient.getClient().create(NewsApiService::class.java)
                val response = apiService.getNewsFeed()
                if (response.isSuccessful && response.body() != null) {
                    _cachedNewsResponse.postValue(response.body())
                    saveToCache(AppConstants.NEWS_CACHE_KEY, response.body())
                } else {
                    Log.e(TAG, "News API response not successful: " + response.code())
                    loadNewsFromCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "News API call failed.", e)
                loadNewsFromCache()
            }
        }
    }

    fun loadNewsFromCache() {
        val prefs = getApplication<Application>().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(AppConstants.NEWS_CACHE_KEY, null)
        if (cachedJson != null) {
            try {
                val news = gson.fromJson(cachedJson, NewsResponse::class.java)
                _cachedNewsResponse.postValue(news)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load news from cache", e)
            }
        }
    }

    fun fetchFunFact() {
        viewModelScope.launch {
            try {
                val api = FunFactRetrofitClient.client.create(FunFactApiService::class.java)
                val response = api.getRandomFact("en")
                val fact = response.body()?.text
                if (response.isSuccessful && fact != null) {
                    //noinspection NullSafeMutableLiveData
                    _funFactText.postValue(fact)
                } else {
                    loadFallbackFunFact()
                }
            } catch (e: Exception) {
                loadFallbackFunFact()
            }
        }
    }

    fun loadFallbackFunFact() {
        try {
            val facts = getApplication<Application>().resources.getStringArray(R.array.fun_facts)
            val idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % facts.size
            _funFactText.postValue(facts[idx])
        } catch (e: Exception) {
            _funFactText.postValue(getApplication<Application>().getString(R.string.fun_fact_fallback))
        }
    }

    fun loadCalendarData() {
        viewModelScope.launch(Dispatchers.IO) {
            _calendarQueryError.postValue(false)
            val events = ArrayList<CalendarEvent>()
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val uri = CalendarContract.Events.CONTENT_URI
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.HOUR_OF_DAY, 24)
                val end = cal.timeInMillis

                val selection = CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTSTART + " <= ?"
                val selectionArgs = arrayOf(now.toString(), end.toString())
                val sort = CalendarContract.Events.DTSTART + " ASC"

                contentResolver.query(uri, CALENDAR_PROJECTION, selection, selectionArgs, sort)?.use { cursor ->
                    _totalEventCount.postValue(cursor.count)
                    val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                    val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                    val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                    val locIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                    val ownerIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                    while (cursor.moveToNext() && events.size < 3) {
                        events.add(CalendarEvent(
                            cursor.getLong(idIdx),
                            cursor.getString(titleIdx),
                            cursor.getLong(startIdx),
                            cursor.getLong(endIdx),
                            cursor.getString(locIdx),
                            cursor.getString(ownerIdx)
                        ))
                    }
                } ?: run {
                    _totalEventCount.postValue(0)
                }
                _calendarEvents.postValue(events)
            } catch (e: Exception) {
                Log.e(TAG, "Cal error", e)
                _calendarQueryError.postValue(true)
                _calendarEvents.postValue(emptyList())
            }
        }
    }

    fun saveSummaryToCache(summary: String) {
        getApplication<Application>().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(AppConstants.KEY_SUMMARY_CACHE, summary).apply()
    }

    private fun saveToCache(key: String, data: Any?) {
        getApplication<Application>().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, gson.toJson(data)).apply()
    }

    override fun onCleared() {
        super.onCleared()
        // Coroutines are cancelled by viewModelScope
    }
}
