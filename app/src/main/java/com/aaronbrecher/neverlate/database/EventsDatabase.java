package com.aaronbrecher.neverlate.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.aaronbrecher.neverlate.models.Event;

@Database(entities = {Event.class}, version = 1)
public abstract class EventsDatabase extends RoomDatabase{
    public abstract EventsDao eventsDao();
}
