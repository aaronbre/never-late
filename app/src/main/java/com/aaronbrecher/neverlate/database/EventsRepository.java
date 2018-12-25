package com.aaronbrecher.neverlate.database;

import androidx.lifecycle.LiveData;

import com.aaronbrecher.neverlate.models.Event;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventsRepository {
    EventsDao mEventsDao;

    @Inject
    public EventsRepository(EventsDao eventsDao) {
        this.mEventsDao = eventsDao;
    }

    public void insertAll(List<Event> events) {
        if(events.size() > 0) mEventsDao.insertAll(events);
    }

    public void insertEvent(Event event) {
        mEventsDao.insertEvent(event);
    }

    //query all events in the database this will include events that are not
    // being watched as well as events without a location
    public LiveData<List<Event>> queryEventsNoLocation() {
        return mEventsDao.queryEventsNoLocation(System.currentTimeMillis());
    }


    //query all the tracked events in the database - events with a watching set to false
    // will NOT be returned - performs async
    public LiveData<List<Event>> queryAllCurrentTrackedEvents() {
        return mEventsDao.queryAllCurrentTrackedEvents(System.currentTimeMillis());
    }

    //query all the tracked events in the database - events with a watching
    //set to false will NOT be returned - performs synchronisly
    public List<Event> queryAllCurrentTrackedEventsSync() {
        return mEventsDao.queryAllCurrentTrackedEventsSync(System.currentTimeMillis());
    }

    //this will return all current events even if the event is not being
    //watched - will not return events without a location
    public List<Event> queryAllCurrentEventsSync(){
        return mEventsDao.queryAllCurrentEventsSync(System.currentTimeMillis());
    }

    //query events for a specific calendar
    public LiveData<List<Event>> queryEventForCalendar(long calId) {
        return mEventsDao.queryEventForCalendar(calId);
    }

    public Event queryEventById(int id) {
        return mEventsDao.queryEventById(id);
    }

    public void deleteEvents(Event... events) {
        mEventsDao.deleteEvents(events);
    }

    public void deleteAllEvents() {
        mEventsDao.deleteAllEvents();
    }

    //delete events for a specific calendar
    public void deleteCalendar(long calId) {
        mEventsDao.deleteCalendar(calId);
    }

    public void updateEvents(Event... events){mEventsDao.updateEvents(events);}
}
