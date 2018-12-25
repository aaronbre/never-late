package com.aaronbrecher.neverlate.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatibility;

@Database(entities = {Event.class, EventCompatibility.class}, version = 7)
@TypeConverters({Converters.class})
public abstract class EventsDatabase extends RoomDatabase{
    public abstract EventsDao eventsDao();
    public abstract EventCompatibilityDao compatabilityDao();
}
