package com.aaronbrecher.neverlate.database;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.aaronbrecher.neverlate.models.Event;

import java.util.List;

@Dao
public interface EventsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Event> events);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEvent(Event event);

    //query all events in the database
    @Query("SELECT * FROM events ORDER BY startTime")
    LiveData<List<Event>> queryAllEvents();

    //query all not expired events in the database
    @Query("SELECT * FROM events WHERE endTime > :currentTime AND location IS NOT NULL AND location != '' ORDER BY startTime")
    LiveData<List<Event>> queryAllCurrentEvents(long currentTime);

    @Query("SELECT * FROM events WHERE endTime > :currentTime ORDER BY startTime")
    List<Event> queryAllCurrentEventsSync(long currentTime);

    @Query("SELECT * FROM events WHERE endTime > :currentTime ORDER BY startTime")
    List<Event> queryEventsNoLocationSync(long currentTime);

    @Query("SELECT * FROM events WHERE id = :id")
    Event queryEventById(int id);

    //query events for a specific calendar
    @Query("SELECT * FROM events WHERE calendarId = :calId ORDER BY startTime")
    LiveData<List<Event>> queryEventForCalendar(long calId);

    @Query("DELETE FROM events")
    void deleteAllEvents();

    //delete events for a specific calendar
    @Query("DELETE FROM events WHERE calendarId = :calId")
    void deleteCalendar(long calId);

    @Delete
    void deleteEvents(Event... event);
}
