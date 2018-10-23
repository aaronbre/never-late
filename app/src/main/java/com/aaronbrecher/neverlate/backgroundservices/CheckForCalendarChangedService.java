package com.aaronbrecher.neverlate.backgroundservices;


import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.Event.Change;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

/**
 * Service which will check peroidically if any new events where added
 * as well as if any event has been changed
 */
public class CheckForCalendarChangedService extends JobService {
    private static final String TAG = CheckForCalendarChangedService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    AppExecutors mAppExecutors;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
        Log.i(TAG, "onCreate: Check for calendar job");
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        mAppExecutors.diskIO().execute(() -> doWork(job));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    private void doWork(final JobParameters job) {
        //need to sort the lists by id rather by time so as for both to be in sync
        //in case a new event was added in a middle time-slot
        List<Event> oldList = mEventsRepository.queryAllCurrentEventsSync();
        Collections.sort(oldList, Event.eventIdComparator);
        List<Event> newList = CalendarUtils.getCalendarEventsForToday(this);
        Collections.sort(newList, Event.eventIdComparator);

        List<Event> eventsToAddWithGeofences = new ArrayList<>();
        List<Event> eventsToAddNoGeofences = new ArrayList<>();

        if (newList.size() > oldList.size()) {
            eventsToAddWithGeofences.addAll(newList.subList(oldList.size(), newList.size()));
        }

        // For each event check if it was changed and add it to the corresponding list
        // events with only a title or description change do not need new fences
        for (int i = 0, listLength = oldList.size(); i < listLength; i++) {
            Event newEvent = newList.get(i);
            Event oldEvent = oldList.get(i);
            Change change = Event.eventChanged(oldEvent, newEvent);
            switch (change) {
                case DESCRIPTION_CHANGE:
                    eventsToAddNoGeofences.add(newEvent);
                    break;
                case GEOFENCE_CHANGE:
                    eventsToAddWithGeofences.add(newEvent);
                    break;
            }
        }

        if (eventsToAddWithGeofences.size() > 0) {
            addGeofenceEvents(eventsToAddWithGeofences);
        }
        if(eventsToAddNoGeofences.size() > 0){
            mEventsRepository.insertAll(eventsToAddNoGeofences);
        }
    }

    /**
     * Add the provided events to the database as well as create fences for
     * said events
     * @param eventsToAddWithGeofences list of events to add to DB and fences
     */
    private void addGeofenceEvents(List<Event> eventsToAddWithGeofences) {
        Location location = null;
        if(mSharedPreferences.contains(Constants.USER_LOCATION_PREFS_KEY)){
            String locationString = mSharedPreferences.getString(Constants.USER_LOCATION_PREFS_KEY, "");
            location = LocationUtils.locationFromLatLngString(locationString);
        }
        DirectionsUtils.addDistanceInfoToEventList(eventsToAddWithGeofences, location);
        mEventsRepository.insertAll(eventsToAddWithGeofences);
        AwarenessFencesCreator fencesCreator = new AwarenessFencesCreator.Builder(eventsToAddWithGeofences).build();
        fencesCreator.buildAndSaveFences();
    }
}
