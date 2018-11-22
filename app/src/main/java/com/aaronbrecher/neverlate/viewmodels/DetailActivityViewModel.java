package com.aaronbrecher.neverlate.viewmodels;

import android.arch.lifecycle.MutableLiveData;

import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;

import javax.inject.Inject;

public class DetailActivityViewModel extends BaseViewModel {
    private MutableLiveData<Event> mEvent;

    @Inject
    public DetailActivityViewModel(EventsRepository eventsRepository){
        super(eventsRepository, null, null);
    }

    public MutableLiveData<Event> getEvent() {
        if (mEvent == null)
            mEvent = new MutableLiveData<>();
        return mEvent;
    }

    public void setEvent(Event event) {
        if (mEvent == null) {
            mEvent = new MutableLiveData<>();
        }
        mEvent.postValue(event);
    }
}
