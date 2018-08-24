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

    public MutableLiveData<List<Event>> getEventsWithLocation() {
        if (mEventsWithLocation == null)
            mEventsWithLocation = new MutableLiveData<>();
        return mEventsWithLocation;
    }

    /**
     * function to set an additional list with updated location based info
     * this function will add all location information to the event list and create a new
     * LiveData object for the fragment to observe. API call is done in AsyncTask in viewModel
     * to prevent multiple calls in orientation change etc.
     * @param events the list of updated events from the getAllCurrentEvents LiveData
     * @param location the current location of the user
     */
    public void setEventsWithLocation(List<Event> events, Location location) {
        if (mEventsWithLocation == null){
            mEventsWithLocation = new MutableLiveData<>();
        }
        boolean isSameList = isSameList(events, mPreviousLocationList);
        if(isSameList) return;
        if(location != null){
            new AsyncTask<Pair<List<Event>, Location>, Void, List<Event>>(){
                @Override
                protected List<Event> doInBackground(Pair<List<Event>, Location>... pairs) {
                    List<Event> eventList = pairs[0].first;
                    Location location = pairs[0].second;
                    addLocations(eventList, location);
                    return eventList;
                }

                @Override
                protected void onPostExecute(List<Event> events) {
                    mEventsWithLocation.postValue(events);
                    mPreviousLocationList = events;
                }
            }.execute(new Pair<>(events, location));
        }else{
            mEventsWithLocation.postValue(events);
        }
    }

    private boolean isSameList(List<Event> list1, List<Event> list2){
        if(list1.size() != list2.size()) return false;
        for(int i = 0; i < list1.size(); i++){
            if(!list1.get(i).equals(list2.get(i))) return false;
        }
        return true;
    }

    /**
     * add the distance and duration to the Event using the Distance Matrix API
     * @param events the list of events to get information about
     * @param location the users current location
     */
    private void addLocations(List<Event> events, Location location){
        DistanceMatrixApiRequest dmRequest = DirectionsUtils.getDistanceMatrixApiRequest(mGeoApiContext, events, location);
        DistanceMatrix distanceMatrix =  null;
        try {
             distanceMatrix = dmRequest.await();
        } catch (InterruptedException | IOException | ApiException e) {
            e.printStackTrace();
        }
        if (distanceMatrix != null) {
            DistanceMatrixElement[] elements = distanceMatrix.rows[0].elements;
            for (int i = 0, j = elements.length; i < j; i++){
                DistanceMatrixElement element = elements[i];
                Event event = events.get(i);
                event.setDistance(element.distance.inMeters);
                //if there is a relative traffic time rather use that
                long timeTo = element.durationInTraffic != null ? element.durationInTraffic.inSeconds : element.duration.inSeconds;
                event.setTimeTo(timeTo);
            }
        }
    }

    //insert the events async using a simple async task
    public void insertGeofences(List<GeofenceModel> geofences) {
        new AsyncTask<List<GeofenceModel>, Void, Void>() {
            @Override
            protected Void doInBackground(List<GeofenceModel>... fences) {
                mGeofencesRepository.insertAll(fences[0]);
                return null;
            }
        }.execute(geofences);
    }

}
