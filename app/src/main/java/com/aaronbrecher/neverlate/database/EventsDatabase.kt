package com.aaronbrecher.neverlate.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.models.EventCompatibility

@Database(entities = [Event::class, EventCompatibility::class], version = 9)
@TypeConverters(Converters::class)
abstract class EventsDatabase : RoomDatabase() {
    abstract fun eventsDao(): EventsDao
    abstract fun compatabilityDao(): EventCompatibilityDao
}
