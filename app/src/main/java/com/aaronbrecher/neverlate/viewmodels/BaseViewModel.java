package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;

public abstract class BaseViewModel extends ViewModel{
    protected Application mApplication;
    protected EventsRepository mEventsRepository;
    private MutableLiveData<Event> mEvent;

    BaseViewModel(EventsRepository eventsRepository, Application application){
        this.mEventsRepository = eventsRepository;
        this.mApplication = application;
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
