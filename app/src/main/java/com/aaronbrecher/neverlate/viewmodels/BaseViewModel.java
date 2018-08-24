package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;

import javax.inject.Inject;

public abstract class BaseViewModel extends ViewModel{
    protected Application mApplication;
    protected EventsRepository mEventsRepository;
    protected GeofencesRepository mGeofencesRepository;
    private MutableLiveData<Event> mEvent;

    BaseViewModel(EventsRepository eventsRepository, GeofencesRepository geofencesRepository, Application application){
        this.mEventsRepository = eventsRepository;
        this.mGeofencesRepository = geofencesRepository;
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

    public LiveData<GeofenceModel> getGeofenceForKey(int id){
        String key = Constants.GEOFENCE_REQUEST_ID + id;
        return mGeofencesRepository.getGeofencebyKey(key);
    }
}
