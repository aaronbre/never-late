package com.aaronbrecher.neverlate.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;

@Database(entities = {Event.class, GeofenceModel.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class EventsDatabase extends RoomDatabase{
    public abstract EventsDao eventsDao();
}
