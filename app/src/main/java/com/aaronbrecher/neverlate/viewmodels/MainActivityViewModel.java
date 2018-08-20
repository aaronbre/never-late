package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Pair;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.backgroundservices.RetrieveCalendarEventsInitialJobService;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MainActivityViewModel extends ViewModel {

    private Application mApplication;
    private EventsRepository mEventsRepository;
    private GeofencesRepository mGeofencesRepository;
    private MutableLiveData<Event> mEvent;
    //this field is to compare previous location so as not to do
    //additional api call on orientation change
    private List<Event> mPreviousLocationList = new ArrayList<>();

    //this field differs from the getAllCurrentEvents as the db is not location aware
    //this field will contain the location info as well (distance and time to travel)
    private MutableLiveData<List<Event>> mEventsWithLocation;


    @Inject
    public MainActivityViewModel(EventsRepository eventsRepository, GeofencesRepository geofencesRepository, Application application) {
        this.mEventsRepository = eventsRepository;
        this.mGeofencesRepository = geofencesRepository;
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
        LiveData<List<Event>> liveData = mEventsRepository.queryAllCurrentEvents();
        return liveData;
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

    private void addLocations(List<Event> events, Location location){
        for (Event event : events) {
            //set the distance to the event using the location
            event.setDistance(getDistance(location, event.getLocation()));
            //get the travel time to the event using the google directions api
            //TODO this uses a LOT of API calls which cost $$$ possibly remove this feature and add to a paid version...
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

    public Job createJob(FirebaseJobDispatcher dispatcher){

        return dispatcher.newJobBuilder()
                .setService(RetrieveCalendarEventsInitialJobService.class)
                .setTag(Constants.CALENDAR_UPDATE_SERVICE_TAG)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(getTriggerTime(),600 ))
                .build();
    }

    private int getTriggerTime(){
        LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        ZonedDateTime zdt = todayMidnight.atZone(ZoneId.systemDefault());
        long midnight =  zdt.toInstant().toEpochMilli();
        return (int) ((midnight - System.currentTimeMillis())/1000);
    }

}
