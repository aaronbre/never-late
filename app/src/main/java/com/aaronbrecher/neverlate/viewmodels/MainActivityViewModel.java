package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Pair;

import com.aaronbrecher.neverlate.BuildConfig;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MainActivityViewModel extends BaseViewModel {
    private MutableLiveData<Event> mEvent;
    //this field is to compare previous location so as not to do
    //additional api call on orientation change
    private List<Event> mPreviousLocationList = new ArrayList<>();

    //this field differs from the getAllCurrentEvents as the db is not location aware
    //this field will contain the location info as well (distance and time to travel)
    private MutableLiveData<List<Event>> mEventsWithLocation;
    private GeoApiContext mGeoApiContext = new GeoApiContext().setApiKey(BuildConfig.GOOGLE_API_KEY);


    @Inject
    public MainActivityViewModel(EventsRepository eventsRepository, GeofencesRepository geofencesRepository, Application application) {
        super(eventsRepository, geofencesRepository, application);
    }

    //insert the events async using a simple async task
    public void insertEvents(List<Event> events) {
        new AsyncTask<List<Event>, Void, Void>() {
            @Override
            protected Void doInBackground(List<Event>... lists) {
                mEventsRepository.insertAll(lists[0]);
                return null;
            }
        }.execute(events);
    }

    public LiveData<List<Event>> getAllCurrentEvents() {
        return mEventsRepository.queryAllCurrentEvents();
    }

    public LiveData<List<Event>> getAllEvents() {
        return mEventsRepository.queryAllEvents();
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
