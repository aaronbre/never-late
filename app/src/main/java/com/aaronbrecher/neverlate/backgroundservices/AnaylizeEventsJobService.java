package com.aaronbrecher.neverlate.backgroundservices;

import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatiblity;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import javax.inject.Inject;

public class AnaylizeEventsJobService extends JobService {
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors mAppExecutors;

    private JobParameters mJobParameters;
    private List<Event> mEventList;
    private List<EventCompatiblity> mEventCompatiblities;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
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
            if(i == mEventList.size()-1) continue;
            mEventCompatiblities.add(isWithinRange(mEventList.get(i), mEventList.get(i+1)));
        }
    }

    private EventCompatiblity isWithinRange(Event event1, Event event2) {
        EventCompatiblity compatiblity = new EventCompatiblity();
        LatLng originLatLng = event1.getLocationLatlng();
        LatLng destinationLatLng = event2.getLocationLatlng();
        long leaveTime = Converters.unixFromDateTime(event1.getStartTime().plusMinutes(10));
        if(originLatLng == null || destinationLatLng == null) return null;

        String origin = originLatLng.longitude + "," + originLatLng.latitude;
        String destination = destinationLatLng.longitude + "," + destinationLatLng.latitude;

        return compatiblity;
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
