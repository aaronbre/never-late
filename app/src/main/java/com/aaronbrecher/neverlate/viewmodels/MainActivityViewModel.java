package com.aaronbrecher.neverlate.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;

import java.util.List;

import javax.inject.Inject;

public class MainActivityViewModel extends ViewModel {

    private EventsRepository mEventsRepository;
    private MutableLiveData<Event> mEvent;

    //this field differs from the getAllCurrentEvents as the db is not location aware
    //this field will contain the location info as well (distance and time to travel)
    private MutableLiveData<List<Event>> mEventsWithLocation;


    @Inject
    public MainActivityViewModel(EventsRepository eventsRepository) {
        this.mEventsRepository = eventsRepository;
    }

    public void insertEvents(List<Event> events){
        mEventsRepository.insertAll(events);
    }

    public LiveData<List<Event>> getAllCurrentEvents(){
        return mEventsRepository.queryAllCurrentEvents();
    }

    public LiveData<List<Event>> getAllEvents(){
        return mEventsRepository.queryAllEvents();
    }

    public MutableLiveData<Event> getEvent(){
        if(mEvent == null)
            mEvent = new MutableLiveData<>();
        return mEvent;
    }

    public void setEvent(Event event) {
        if (mEvent == null){
            mEvent = new MutableLiveData<>();
        }
        mEvent.postValue(event);
    }

    public MutableLiveData<List<Event>> getEventsWithLocation() {
        if(mEventsWithLocation == null)
            mEventsWithLocation = new MutableLiveData<>();
        return mEventsWithLocation;
    }

    public void setEventsWithLocation(List<Event> eventsWithLocation) {
        if(mEventsWithLocation == null)
            mEventsWithLocation = new MutableLiveData<>();
        mEventsWithLocation.postValue(eventsWithLocation);
    }
}
