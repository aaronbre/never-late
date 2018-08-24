package com.aaronbrecher.neverlate.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;

import javax.inject.Inject;

public class DetailActivityViewModel extends BaseViewModel {
    private MutableLiveData<Event> mEvent;

    @Inject
    public DetailActivityViewModel(EventsRepository eventsRepository, GeofencesRepository geofencesRepository){
        super(eventsRepository, geofencesRepository, null);
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
