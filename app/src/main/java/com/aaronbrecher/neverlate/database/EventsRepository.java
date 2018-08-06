package com.aaronbrecher.neverlate.database;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.aaronbrecher.neverlate.models.Event;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventsRepository {
    EventsDao mEventsDao;

    @Inject
    public EventsRepository(EventsDao eventsDao){
        this.mEventsDao = eventsDao;
    }

    public void insertAll(List<Event> events){
        mEventsDao.insertAll(events);
    }

    public void insertEvent(Event event){
        mEventsDao.insertEvent(event);
    }

    //query all events in the database
    public LiveData<List<Event>> queryAllEvents(){
        return mEventsDao.queryAllEvents();
    }

    //query events for a specific calendar
    public LiveData<List<Event>> queryEventForCalendar(long calId){
        return mEventsDao.queryEventForCalendar(calId);
    }

    public Event queryEventById(int id){
        return mEventsDao.queryEventById(id);
    }

    public void deleteAllEvents(){
        mEventsDao.deleteAllEvents();
    }

    //delete events for a specific calendar
    public void deleteCalendar(long calId){
        mEventsDao.deleteCalendar(calId);
    }
}
