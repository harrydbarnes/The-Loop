package com.example.theloop

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.theloop.data.local.dao.WeatherDao
import com.example.theloop.models.WeatherResponse
import com.example.theloop.utils.AppConstants
import com.example.theloop.utils.AppUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DayAheadWidget : AppWidgetProvider() {

    @Inject
    lateinit var weatherDao: WeatherDao

    @Inject
    lateinit var gson: Gson

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val weatherEntity = weatherDao.getWeather()
                val summary = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(AppConstants.KEY_SUMMARY_CACHE, context.getString(R.string.widget_default_summary))

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, weatherEntity?.json, summary, gson)
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
        weatherJson: String?,
        summary: String?,
        gson: Gson
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_day_ahead)
        views.setTextViewText(R.id.widget_summary, summary)

        if (weatherJson != null) {
            try {
                val weather = gson.fromJson(weatherJson, WeatherResponse::class.java)
                val current = weather.current
                views.setTextViewText(R.id.widget_temp, "%.0f°".format(current.temperature))
                views.setImageViewResource(R.id.widget_weather_icon, AppUtils.getWeatherIconResource(current.weatherCode))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
