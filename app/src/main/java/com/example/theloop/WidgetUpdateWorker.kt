package com.example.theloop

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.theloop.data.repository.CalendarRepository
import com.example.theloop.data.repository.NewsRepository
import com.example.theloop.data.repository.WeatherRepository
import com.example.theloop.utils.AppConstants
import com.example.theloop.utils.SummaryUtils
import com.example.theloop.DayAheadWidget
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.ArrayList

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val weatherRepo: WeatherRepository,
    private val newsRepo: NewsRepository,
    private val calendarRepo: CalendarRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val latStr = prefs.getString(AppConstants.KEY_LATITUDE, null)
        val lonStr = prefs.getString(AppConstants.KEY_LONGITUDE, null)
        val unit = prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT) ?: AppConstants.DEFAULT_TEMP_UNIT
        val userName = prefs.getString(AppConstants.KEY_USER_NAME, "User") ?: "User"

        if (latStr == null || lonStr == null) {
            return Result.success()
        }

        return try {
            val lat = latStr.toDouble()
            val lon = lonStr.toDouble()

            // Update Data (saves to DB)
            val weatherSuccess = weatherRepo.refresh(lat, lon, unit)
            val newsSuccess = newsRepo.refreshNews()
            val calendarSuccess = calendarRepo.refreshEvents()

            // Get latest data for summary
            val weather = weatherRepo.weatherData.first()
            val news = newsRepo.getArticles("US").first()
            val events = calendarRepo.events.first()

            // Generate Summary
            val summary = if (weather != null) {
                 SummaryUtils.generateSummary(
                    applicationContext,
                    weather,
                    events,
                    events.size,
                    news.firstOrNull(),
                    userName,
                    false
                )
            } else null

            if (summary != null) {
                prefs.edit().putString(AppConstants.KEY_SUMMARY_CACHE, summary).apply()
            }

            // Trigger widget update
            val intent = Intent(applicationContext, DayAheadWidget::class.java)
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val ids = AppWidgetManager.getInstance(applicationContext)
                .getAppWidgetIds(ComponentName(applicationContext, DayAheadWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            applicationContext.sendBroadcast(intent)

            if (!weatherSuccess || !newsSuccess || !calendarSuccess) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateWorker", "Widget update failed", e)
            Result.retry()
        }
    }
}
