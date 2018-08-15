package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;

import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class MainActivityViewModel extends ViewModel {

    private Application mApplication;
    private EventsRepository mEventsRepository;
    private MutableLiveData<Event> mEvent;

    //this field differs from the getAllCurrentEvents as the db is not location aware
    //this field will contain the location info as well (distance and time to travel)
    private MutableLiveData<List<Event>> mEventsWithLocation;


    @Inject
    public MainActivityViewModel(EventsRepository eventsRepository, Application application) {
        this.mEventsRepository = eventsRepository;
        this.mApplication = application;
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

    public MutableLiveData<List<Event>> getEventsWithLocation() {
        if (mEventsWithLocation == null)
            mEventsWithLocation = new MutableLiveData<>();
        return mEventsWithLocation;
    }

    public void setEventsWithLocation(List<Event> eventsWithLocation) {
        if (mEventsWithLocation == null)
            mEventsWithLocation = new MutableLiveData<>();
        mEventsWithLocation.postValue(eventsWithLocation);
    }

    private boolean compareLists(List<Event> list1, List<Event> list2){
        if(list1.size() != list2.size()) return false;
        for(int i = 0; i < list1.size(); i++){
            if(!list1.get(i).equals(list2.get(i))) return false;
        }
        return true;
    }

    private void addLocations(List<Event> events, Location location){
        for (Event event : events) {
            //set the distance to the event using the location
            event.setDistance(getDistance(location, event.getLocation()));
            //get the travel time to the event using the google directions api this blocks the
            //main UI thread as all information is needed to update the viewModel
            //TODO see if there is a better way...
            DirectionsApiRequest apiRequest = DirectionsUtils.getDirectionsApiRequest(
                    LocationUtils.latlngFromAddress(mApplication.getApplicationContext(), event.getLocation()),
                    LocationUtils.locationToLatLng(location));
            try {
                DirectionsResult result = apiRequest.await();
                event.setTimeTo(result.routes[0].legs[0].duration.humanReadable);
            } catch (ApiException | InterruptedException | IOException e) {
                e.printStackTrace();
            }

        }
    }

    private String getDistance(Location location, String destinationAddress) {
        LatLng latLng = LocationUtils.latlngFromAddress(mApplication.getApplicationContext(), destinationAddress);
        Location destination = LocationUtils.latlngToLocation(latLng);
        return LocationUtils.getDistance(location, destination);
    }
}
