package com.aaronbrecher.neverlate.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;

import java.util.List;

import javax.inject.Inject;

public class MainActivityViewModel extends ViewModel {

    private EventsRepository mEventsRepository;

    @Inject
    public MainActivityViewModel(EventsRepository eventsRepository) {
        this.mEventsRepository = eventsRepository;
    }

    public void insertEvent(Event event){
        mEventsRepository.insertEvent(event);
    }

    public LiveData<List<Event>> getAllEvents(){
        return mEventsRepository.queryAllEvents();
    }
}
