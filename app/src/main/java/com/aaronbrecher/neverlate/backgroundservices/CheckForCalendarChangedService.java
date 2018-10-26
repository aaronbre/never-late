package com.aaronbrecher.neverlate.backgroundservices;


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
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
import com.google.android.gms.location.FusedLocationProviderClient;

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

    @Inject
    FusedLocationProviderClient mLocationProviderClient;

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
            addDistanceDataAndCreateFences(geofenceList);
        }
        //combine the lists
        geofenceList.addAll(noGeofenceList);
        mEventsRepository.deleteAllEvents();
        mEventsRepository.insertAll(geofenceList);
        jobFinished(job, false);
    }

    /**
     * Add the provided events to the database as well as create fences for
     * said events, in the event that location data cannot be added will remove old fences
     * as they are no longer relevant
     * @param eventsToAddWithGeofences list of events to add to DB and fences
     */
    private void addDistanceDataAndCreateFences(List<Event> eventsToAddWithGeofences) {
        Location location = null;
        if (mSharedPreferences.contains(Constants.USER_LOCATION_PREFS_KEY)) {
            String locationString = mSharedPreferences.getString(Constants.USER_LOCATION_PREFS_KEY, "");
            location = LocationUtils.locationFromLatLngString(locationString);
        }
        if (location != null) {
            setOrRemoveFences(eventsToAddWithGeofences, location);
        }
        //if there is no location will try to get it here
        //TODO there is a bug here that the callback will be executed on the main thread
        //TODO also the callback will happen after the db was already updated need to fix
        else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) return;
            mLocationProviderClient.getLastLocation().addOnSuccessListener(newLocation -> {
                if(newLocation != null){
                    mSharedPreferences.edit().putString(Constants.USER_LOCATION_PREFS_KEY, LocationUtils.locationToLatLngString(newLocation)).apply();
                }
                setOrRemoveFences(eventsToAddWithGeofences, newLocation);
            });
        }
    }

    private void setOrRemoveFences(List<Event> eventsToAddWithGeofences, Location location) {
        boolean wasAdded = false;
        AwarenessFencesCreator fencesCreator = new AwarenessFencesCreator.Builder(null).build();
        wasAdded = DirectionsUtils.addDistanceInfoToEventList(eventsToAddWithGeofences, location);
        if (wasAdded){
            fencesCreator.setEventList(eventsToAddWithGeofences);
            fencesCreator.buildAndSaveFences();
        }
        else fencesCreator.removeFences(eventsToAddWithGeofences.toArray(new Event[0]));
    }
}
