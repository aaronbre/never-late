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

public class DetailActivityViewModel extends ViewModel {
    private EventsRepository mEventsRepository;
    private GeofencesRepository mGeofencesRepository;
    private Event mEvent;

    @Inject
    public DetailActivityViewModel(EventsRepository eventsRepository, GeofencesRepository geofencesRepository){
        this.mEventsRepository = eventsRepository;
        this.mGeofencesRepository = geofencesRepository;
    }

    public Event getEvent(){
        return mEvent;
    }

    public void setEvent(Event event) {
        mEvent = event;
    }

    public LiveData<GeofenceModel> getGeofenceForKey(int id){
        String key = Constants.GEOFENCE_REQUEST_ID + id;
        return mGeofencesRepository.getGeofencebyKey(key);
    }
}
