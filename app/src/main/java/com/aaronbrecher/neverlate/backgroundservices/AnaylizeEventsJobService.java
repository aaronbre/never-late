package com.aaronbrecher.neverlate.backgroundservices;

import android.location.Location;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatibility;
import com.aaronbrecher.neverlate.models.retrofitmodels.MapboxDirectionMatrix;
import com.aaronbrecher.neverlate.network.AppApiService;
import com.aaronbrecher.neverlate.network.AppApiUtils;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class AnaylizeEventsJobService extends JobService {
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors mAppExecutors;

    private AppApiService mApiService;
    private JobParameters mJobParameters;
    private List<Event> mEventList;
    private List<EventCompatibility> mMEventCompatibilities;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
        mApiService = AppApiUtils.createService();
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        mJobParameters = job;
        mAppExecutors.diskIO().execute(this::doWork);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    private void doWork(){
        mEventList = mEventsRepository.queryAllCurrentEventsSync();
        for(int i = 0; i < mEventList.size(); i++){
            if(i != mEventList.size()-1){
                mMEventCompatibilities.add(getCompatibility(mEventList.get(i), mEventList.get(i+1)));
            }
        }
    }

    private EventCompatibility getCompatibility(Event event1, Event event2) {
        EventCompatibility eventCompatibility = new EventCompatibility();
        eventCompatibility.setStartEvent(event1);
        eventCompatibility.setEndEvent(event2);

        LatLng originLatLng = event1.getLocationLatlng();
        LatLng destinationLatLng = event2.getLocationLatlng();
        if(originLatLng == null || destinationLatLng == null) return null;
        String origin = originLatLng.longitude + "," + originLatLng.latitude;
        String destination = destinationLatLng.longitude + "," + destinationLatLng.latitude;
        double duration = getMapboxDrivingDuration(origin, destination) * 1000;
        if(duration < 0) eventCompatibility.setWithinDrivingDistance(EventCompatibility.Compatible.UNKNOWN);
        else {
            determineComparabilityAndTiming(eventCompatibility, duration);
        }
        return eventCompatibility;
    }

    /**
     * get the mapbox distance
     */
    private double getMapboxDrivingDuration(String origin, String destination){
        Call<MapboxDirectionMatrix> call = mApiService.queryMapboxDirectionMatrix(origin, destination, 1);
        try{
            MapboxDirectionMatrix matrix = call.execute().body();
            if(matrix == null || matrix.getDurations().size() < 1) return -1;
            double duration = matrix.getDurations().get(0).get(0);
            return duration;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void determineComparabilityAndTiming(EventCompatibility eventCompatibility, double duration) {
        long firstEventStart = Converters.unixFromDateTime(eventCompatibility.getStartEvent().getStartTime());
        long secondEventStart = Converters.unixFromDateTime(eventCompatibility.getEndEvent().getStartTime());
        long arrivalTimeToSecondEvent = firstEventStart + (long)duration;
        if(arrivalTimeToSecondEvent > secondEventStart){
            eventCompatibility.setWithinDrivingDistance(EventCompatibility.Compatible.FALSE);
            eventCompatibility.setCanReturnHome(false);
            eventCompatibility.setCanReturnToWork(false);
            return;
        }else {
            eventCompatibility.setWithinDrivingDistance(EventCompatibility.Compatible.TRUE);
            long maximumTimeAtEvent = (secondEventStart - arrivalTimeToSecondEvent)/1000;
            eventCompatibility.setMaxTimeAtStartEvent((int) maximumTimeAtEvent);
        }

    }

    /**
     * Gets the "as the crow flies" distance between two events
     * @return the distance in KM
     */
    private float getCrowFlyDistance(LatLng origin, LatLng destination){
        Location locationorigin = new Location("never-late");
        Location locationDest = new Location("never-late");
        locationorigin.setLatitude(origin.latitude);
        locationorigin.setLongitude(origin.longitude);
        locationDest.setLatitude(destination.latitude);
        locationDest.setLongitude(origin.longitude);
        return locationorigin.distanceTo(locationDest)/1000;

    }
}
