package com.aaronbrecher.neverlate.viewmodels;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;

import javax.inject.Inject;

public class DetailActivityViewModel extends ViewModel {
    private EventsRepository mEventsRepository;
    private MutableLiveData<Event> mEvent;

    @Inject
    public DetailActivityViewModel(EventsRepository eventsRepository){
        this.mEventsRepository = eventsRepository;
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
}
