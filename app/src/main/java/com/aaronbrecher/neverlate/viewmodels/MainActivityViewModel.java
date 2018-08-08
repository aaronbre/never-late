package com.aaronbrecher.neverlate.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.database.Cursor;

import com.aaronbrecher.neverlate.Utils.CalendarUtils;
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

    public void insertEvents(List<Event> events){
        mEventsRepository.insertAll(events);
    }

    public LiveData<List<Event>> getAllCurrentEvents(){
        return mEventsRepository.queryAllCurrentEvents();
    }

}
