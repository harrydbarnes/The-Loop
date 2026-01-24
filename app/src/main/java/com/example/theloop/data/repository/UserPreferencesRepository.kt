package com.example.theloop.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.theloop.R
import com.example.theloop.utils.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    private val prefsChangeFlow = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            trySend(key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(null) }.conflate()

    val tempUnit: Flow<String> = prefsChangeFlow
        .map { prefs.getString(AppConstants.KEY_TEMP_UNIT, AppConstants.DEFAULT_TEMP_UNIT) ?: "celsius" }
        .distinctUntilChanged()

    val userName: Flow<String> = prefsChangeFlow
        .map { prefs.getString(AppConstants.KEY_USER_NAME, "User") ?: "User" }
        .distinctUntilChanged()

    val summary: Flow<String> = prefsChangeFlow
        .map { prefs.getString(AppConstants.KEY_SUMMARY_CACHE, context.getString(R.string.widget_default_summary)) ?: context.getString(R.string.widget_default_summary) }
        .distinctUntilChanged()

    val location: Flow<Pair<Double, Double>> = prefsChangeFlow
        .map {
            val latStr = prefs.getString(AppConstants.KEY_LATITUDE, AppConstants.DEFAULT_LATITUDE.toString())
            val lonStr = prefs.getString(AppConstants.KEY_LONGITUDE, AppConstants.DEFAULT_LONGITUDE.toString())
            val lat = latStr?.toDoubleOrNull() ?: AppConstants.DEFAULT_LATITUDE
            val lon = lonStr?.toDoubleOrNull() ?: AppConstants.DEFAULT_LONGITUDE
            lat to lon
        }
        .distinctUntilChanged()

    fun updateLocation(lat: Double, lon: Double) {
        prefs.edit()
            .putString(AppConstants.KEY_LATITUDE, lat.toString())
            .putString(AppConstants.KEY_LONGITUDE, lon.toString())
            .apply()
    }

    fun saveSummary(summary: String) {
        prefs.edit().putString(AppConstants.KEY_SUMMARY_CACHE, summary).apply()
    }

    fun saveUserName(name: String) {
        prefs.edit().putString(AppConstants.KEY_USER_NAME, name).apply()
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(AppConstants.KEY_ONBOARDING_COMPLETED, true).apply()
    }

    fun hasLocation(): Boolean {
        return prefs.contains(AppConstants.KEY_LATITUDE) && prefs.contains(AppConstants.KEY_LONGITUDE)
    }

    // Helper to get current location synchronously if needed (though Flow is better)
    fun getLocationSync(): Pair<Double, Double> {
        val latStr = prefs.getString(AppConstants.KEY_LATITUDE, AppConstants.DEFAULT_LATITUDE.toString())
        val lonStr = prefs.getString(AppConstants.KEY_LONGITUDE, AppConstants.DEFAULT_LONGITUDE.toString())
        val lat = latStr?.toDoubleOrNull() ?: AppConstants.DEFAULT_LATITUDE
        val lon = lonStr?.toDoubleOrNull() ?: AppConstants.DEFAULT_LONGITUDE
        return lat to lon
    }
}
