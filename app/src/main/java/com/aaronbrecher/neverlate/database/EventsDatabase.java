package com.aaronbrecher.neverlate.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatibility;

@Database(entities = {Event.class, EventCompatibility.class}, version = 4)
@TypeConverters({Converters.class})
public abstract class EventsDatabase extends RoomDatabase{
    public abstract EventsDao eventsDao();
    public abstract EventCompatibilityDao compatabilityDao();
}
