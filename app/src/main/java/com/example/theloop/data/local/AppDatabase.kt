package com.example.theloop.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.theloop.data.local.dao.ArticleDao
import com.example.theloop.data.local.dao.CalendarEventDao
import com.example.theloop.data.local.dao.WeatherDao
import com.example.theloop.data.local.entity.ArticleEntity
import com.example.theloop.data.local.entity.CalendarEventEntity
import com.example.theloop.data.local.entity.WeatherEntity

@Database(entities = [WeatherEntity::class, ArticleEntity::class, CalendarEventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun articleDao(): ArticleDao
    abstract fun calendarEventDao(): CalendarEventDao
}
