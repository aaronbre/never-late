package com.aaronbrecher.neverlate.database;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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
    @Query("SELECT * FROM events WHERE endTime > :currentTime AND location IS NOT NULL AND location != '' AND watching = 1 ORDER BY startTime")
    LiveData<List<Event>> queryAllCurrentTrackedEvents(long currentTime);

    //query all not expired events in the database
    @Query("SELECT * FROM events WHERE endTime > :currentTime AND location IS NOT NULL AND location != '' AND watching = 1 ORDER BY startTime")
    List<Event> queryAllCurrentTrackedEventsSync(long currentTime);

    @Query("SELECT * FROM events WHERE endTime > :currentTime AND location IS NOT NULL AND location != '' ORDER BY startTime")
    List<Event> queryAllCurrentEventsSync(long currentTime);

    @Query("SELECT * FROM events WHERE endTime > :currentTime ORDER BY startTime")
    LiveData<List<Event>> queryEventsNoLocation(long currentTime);

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

    @Update
    void updateEvents(Event... event);
}
