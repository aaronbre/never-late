package com.aaronbrecher.neverlate.backgroundservices;


import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        List<Event> oldList = mEventsRepository.queryAllCurrentEventsSync();
        List<Event> newList = CalendarUtils.getCalendarEventsForToday(this);

        HashMap<String, List<Event>> listsToAdd = CalendarUtils.compareCalendars(oldList, newList);
        List<Event> geofenceList = listsToAdd.get(Constants.LIST_NEEDS_FENCE_UPDATE);
        List<Event> noGeofenceList = listsToAdd.get(Constants.LIST_NO_FENCE_UPDATE);

        if (geofenceList.size() > 0) {
            addDistanceData(geofenceList);
        }
        //combine the lists
        geofenceList.addAll(noGeofenceList);
        mEventsRepository.deleteAllEvents();
        mEventsRepository.insertAll(geofenceList);
    }

    /**
     * Add the provided events to the database as well as create fences for
     * said events
     * @param eventsToAddWithGeofences list of events to add to DB and fences
     */
    private void addDistanceData(List<Event> eventsToAddWithGeofences) {
        Location location = null;
        if(mSharedPreferences.contains(Constants.USER_LOCATION_PREFS_KEY)){
            String locationString = mSharedPreferences.getString(Constants.USER_LOCATION_PREFS_KEY, "");
            location = LocationUtils.locationFromLatLngString(locationString);
        }
        if(location != null){
            DirectionsUtils.addDistanceInfoToEventList(eventsToAddWithGeofences, location);
        }
        AwarenessFencesCreator fencesCreator = new AwarenessFencesCreator.Builder(eventsToAddWithGeofences).build();
        fencesCreator.buildAndSaveFences();
    }
}
