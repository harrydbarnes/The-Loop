package com.example.theloop.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class HealthConnectHelper(private val context: Context) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    interface StepsCallback {
        fun onStepsFetched(steps: Long)
        fun onError(e: Exception)
    }

    fun fetchStepsToday(callback: StepsCallback) {
        val client = HealthConnectClient.getOrCreate(context)
        scope.launch {
            try {
                val end = LocalDateTime.now()
                val start = end.truncatedTo(ChronoUnit.DAYS)

                val response = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )

                val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
                withContext(Dispatchers.Main) {
                    callback.onStepsFetched(steps)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    fun cancel() {
        job.cancel()
    }
}
