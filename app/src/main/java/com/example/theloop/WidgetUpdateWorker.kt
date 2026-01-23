package com.example.theloop

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.theloop.data.repository.CalendarRepository
import com.example.theloop.data.repository.NewsRepository
import com.example.theloop.data.repository.UserPreferencesRepository
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
    private val calendarRepo: CalendarRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!userPreferencesRepository.hasLocation()) {
            return Result.success()
        }

        return try {
            val (lat, lon) = userPreferencesRepository.location.first()
            val unit = userPreferencesRepository.tempUnit.first()
            val userName = userPreferencesRepository.userName.first()

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
                userPreferencesRepository.saveSummary(summary)
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
