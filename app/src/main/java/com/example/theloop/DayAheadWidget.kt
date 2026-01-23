package com.example.theloop

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.theloop.data.repository.UserPreferencesRepository
import com.example.theloop.data.repository.WeatherRepository
import com.example.theloop.models.WeatherResponse
import com.example.theloop.utils.AppConstants
import com.example.theloop.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DayAheadWidget : AppWidgetProvider() {

    @Inject
    lateinit var weatherRepository: WeatherRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val weather = weatherRepository.weatherData.first()
                val summary = userPreferencesRepository.summary.first()

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, weather, summary)
                }
            } catch (e: Exception) {
                android.util.Log.e("DayAheadWidget", "Error in onUpdate", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        weather: WeatherResponse?,
        summary: String?
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_day_ahead)
        views.setTextViewText(R.id.widget_summary, summary)

        if (weather != null) {
            try {
                val current = weather.current
                views.setTextViewText(R.id.widget_temp, "%.0f°".format(current.temperature))
                views.setImageViewResource(R.id.widget_weather_icon, AppUtils.getWeatherIconResource(current.weatherCode))
            } catch (e: Exception) {
                android.util.Log.e("DayAheadWidget", "Error processing weather data", e)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
