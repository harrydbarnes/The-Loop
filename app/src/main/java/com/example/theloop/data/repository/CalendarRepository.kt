package com.example.theloop.data.repository

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.theloop.data.local.dao.CalendarEventDao
import com.example.theloop.data.local.entity.CalendarEventEntity
import com.example.theloop.models.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CalendarEventDao
) {
    val events: Flow<List<CalendarEvent>> = dao.getEventsFlow().map { entities ->
        entities.map {
            CalendarEvent(it.id, it.title, it.startTime, it.endTime, it.location, it.ownerName)
        }
    }

    suspend fun refreshEvents(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_CALENDAR
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext true
                }

                val events = ArrayList<CalendarEventEntity>()
                val contentResolver = context.contentResolver
                val uri = CalendarContract.Events.CONTENT_URI
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.HOUR_OF_DAY, 24)
                val end = cal.timeInMillis

                val selection =
                    CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTSTART + " <= ?"
                val selectionArgs = arrayOf(now.toString(), end.toString())
                val sort = CalendarContract.Events.DTSTART + " ASC"

                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_LOCATION,
                    CalendarContract.Events.CALENDAR_DISPLAY_NAME
                )

                contentResolver.query(uri, projection, selection, selectionArgs, sort)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                    val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                    val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                    val locIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                    val ownerIdx =
                        cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                    while (cursor.moveToNext() && events.size < 5) { // Limit to 5
                        events.add(
                            CalendarEventEntity(
                                id = cursor.getLong(idIdx),
                                title = cursor.getString(titleIdx) ?: "No Title",
                                startTime = cursor.getLong(startIdx),
                                endTime = cursor.getLong(endIdx),
                                location = cursor.getString(locIdx),
                                ownerName = cursor.getString(ownerIdx)
                            )
                        )
                    }
                }

                dao.clearAll()
                if (events.isNotEmpty()) {
                    dao.insertEvents(events)
                }
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing events", e)
                return@withContext false
            }
        }
    }

    companion object {
        private const val TAG = "CalendarRepository"
    }
}
