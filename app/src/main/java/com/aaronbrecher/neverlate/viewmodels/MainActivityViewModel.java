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
    private MutableLiveData<Location> mLocation;


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

    public MutableLiveData<Location> getLocation(){
        if(mLocation == null)
            mLocation = new MutableLiveData<>();
        return mLocation;
    }

    public void setLocation(Location location) {
        if (mLocation == null){
            mLocation = new MutableLiveData<>();
        }
        mLocation.postValue(location);
    }

}
