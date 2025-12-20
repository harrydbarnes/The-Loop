package com.example.theloop

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.theloop.models.Article
import com.example.theloop.models.CalendarEvent
import com.example.theloop.models.NewsResponse
import com.example.theloop.network.RetrofitClient
import com.example.theloop.network.WeatherApiService
import com.example.theloop.utils.AppConstants
import com.example.theloop.utils.SummaryUtils
import com.google.gson.Gson
import java.io.IOException
import java.util.ArrayList

class WidgetUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val TAG = "WidgetUpdateWorker"
    private val gson = Gson()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Fetching weather for widget update...")

        val prefs = applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

        val latStr = prefs.getString(AppConstants.KEY_LATITUDE, null)
        val lonStr = prefs.getString(AppConstants.KEY_LONGITUDE, null)

        if (latStr == null || lonStr == null) {
            Log.w(TAG, "No location available for widget update. Skipping weather fetch.")
            return Result.success()
        }

        val lat: Double
        val lon: Double
        try {
            lat = latStr.toDouble()
            lon = lonStr.toDouble()
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Could not parse lat/lon from SharedPreferences", e)
            return Result.failure()
        }

        // Fetch Weather
        try {
            val unit = prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT) ?: AppConstants.DEFAULT_TEMP_UNIT

            val apiService = RetrofitClient.getClient().create(WeatherApiService::class.java)
            val response = apiService.getWeather(
                lat, lon,
                "temperature_2m,weather_code",
                "weather_code,temperature_2m_max,temperature_2m_min",
                unit,
                "auto"
            )

            if (response.isSuccessful && response.body() != null) {
                // Save to cache
                val json = gson.toJson(response.body())
                prefs.edit().putString(AppConstants.WEATHER_CACHE_KEY, json).apply()

                // Generate Summary
                try {
                    val newsJson = prefs.getString(AppConstants.KEY_NEWS_CACHE, null)
                    var topHeadline: Article? = null
                    if (newsJson != null) {
                        try {
                            val news = gson.fromJson(newsJson, NewsResponse::class.java)
                            if (news != null && news.us != null && news.us.isNotEmpty()) {
                                topHeadline = news.us[0]
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse news cache", e)
                        }
                    }

                    // Calendar logic skipped for background worker (using empty list)
                    val events = ArrayList<CalendarEvent>()
                    val totalEvents = 0
                    val calendarError = false
                    val userName = prefs.getString(AppConstants.KEY_USER_NAME, "User") ?: "User"

                    val summary = SummaryUtils.generateSummary(
                        applicationContext,
                        response.body(),
                        events,
                        totalEvents,
                        topHeadline,
                        userName,
                        calendarError
                    )

                    prefs.edit().putString(AppConstants.KEY_SUMMARY_CACHE, summary).apply()

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate summary in worker", e)
                }

                // Trigger widget update explicitly
                val intent = android.content.Intent(applicationContext, DayAheadWidget::class.java)
                intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                val ids = android.appwidget.AppWidgetManager.getInstance(applicationContext)
                    .getAppWidgetIds(android.content.ComponentName(applicationContext, DayAheadWidget::class.java))
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                applicationContext.sendBroadcast(intent)

                return Result.success()
            } else {
                Log.w(TAG, "Widget weather fetch failed with code: " + response.code())
                if (response.code() >= 500) {
                    return Result.retry()
                }
                return Result.failure()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Widget update failed", e)
            return Result.retry()
        } catch (e: Exception) {
             Log.e(TAG, "Widget update failed with unexpected exception", e)
             return Result.failure()
        }
    }
}
