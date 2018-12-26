package com.aaronbrecher.neverlate.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import com.aaronbrecher.neverlate.models.Event

@Dao
interface EventsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(events: List<Event>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: Event)

    //query all events in the database
    @Query("SELECT * FROM events ORDER BY startTime")
    fun queryAllEvents(): LiveData<List<Event>>

    //query all not expired events in the database
    @Query("SELECT * FROM events WHERE endTime > :currentTime AND location IS NOT NULL AND location != '' AND watching = 1 ORDER BY startTime")
    fun queryAllCurrentTrackedEvents(currentTime: Long): LiveData<List<Event>>

    //query all not expired events in the database
    @Query("SELECT * FROM events WHERE endTime > :currentTime AND location IS NOT NULL AND location != '' AND watching = 1 ORDER BY startTime")
    fun queryAllCurrentTrackedEventsSync(currentTime: Long): List<Event>

    @Query("SELECT * FROM events WHERE endTime > :currentTime AND location IS NOT NULL AND location != '' ORDER BY startTime")
    fun queryAllCurrentEventsSync(currentTime: Long): List<Event>

    @Query("SELECT * FROM events WHERE endTime > :currentTime ORDER BY startTime")
    fun queryEventsNoLocation(currentTime: Long): LiveData<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun queryEventById(id: Int): Event

    //query events for a specific calendar
    @Query("SELECT * FROM events WHERE calendarId = :calId ORDER BY startTime")
    fun queryEventForCalendar(calId: Long): LiveData<List<Event>>

    @Query("DELETE FROM events")
    fun deleteAllEvents()

    //delete events for a specific calendar
    @Query("DELETE FROM events WHERE calendarId = :calId")
    fun deleteCalendar(calId: Long)

    @Delete
    fun deleteEvents(vararg event: Event)

    @Update
    fun updateEvents(vararg event: Event)
}
