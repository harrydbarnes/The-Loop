package com.example.theloop.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val ownerName: String?
)
