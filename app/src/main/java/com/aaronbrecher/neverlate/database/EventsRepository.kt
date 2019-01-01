package com.aaronbrecher.neverlate.database

import androidx.lifecycle.LiveData

import com.aaronbrecher.neverlate.models.Event

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject
constructor(private var mEventsDao: EventsDao) {

    fun insertAll(events: List<Event>) {
        if (events.isNotEmpty()) mEventsDao.insertAll(events)
    }

    fun insertEvent(event: Event) {
        mEventsDao.insertEvent(event)
    }

    //query all events in the database this will include events that are not
    // being watched as well as events without a location
    fun queryEventsNoLocation(): LiveData<List<Event>> {
        return mEventsDao.queryEventsNoLocation(System.currentTimeMillis())
    }

    fun queryEventsNoLocationSync(): List<Event>{
        return mEventsDao.queryAllEventsSync()
    }


    //query all the tracked events in the database - events with a watching set to false
    // will NOT be returned - performs async
    fun queryAllCurrentTrackedEvents(): LiveData<List<Event>> {
        return mEventsDao.queryAllCurrentTrackedEvents(System.currentTimeMillis())
    }

    //query all the tracked events in the database - events with a watching
    //set to false will NOT be returned - performs synchronisly
    fun queryAllCurrentTrackedEventsSync(): List<Event> {
        return mEventsDao.queryAllCurrentTrackedEventsSync(System.currentTimeMillis())
    }

    //this will return all current events even if the event is not being
    //watched - will not return events without a location
    fun queryAllCurrentEventsSync(): List<Event> {
        return mEventsDao.queryAllCurrentEventsSync(System.currentTimeMillis())
    }

    //query events for a specific calendar
    fun queryEventForCalendar(calId: Long): LiveData<List<Event>> {
        return mEventsDao.queryEventForCalendar(calId)
    }

    fun queryEventById(id: Int): Event {
        return mEventsDao.queryEventById(id)
    }

    fun deleteEvents(vararg events: Event) {
        mEventsDao.deleteEvents(*events)
    }

    fun deleteAllEvents() {
        mEventsDao.deleteAllEvents()
    }

    //delete events for a specific calendar
    fun deleteCalendar(calId: Long) {
        mEventsDao.deleteCalendar(calId)
    }

    fun updateEvents(vararg events: Event) {
        mEventsDao.updateEvents(*events)
    }
}
