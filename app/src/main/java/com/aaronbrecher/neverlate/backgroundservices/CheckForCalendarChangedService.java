package com.aaronbrecher.neverlate.backgroundservices;


import android.Manifest;
import android.annotation.SuppressLint;
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
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
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

    private JobParameters mJobParameters;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
        Log.i(TAG, "onCreate: Check for calendar job");
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

    @SuppressLint("MissingPermission")
    private void doWork() {
        Log.i(TAG, "doWork: checking for calendar changes");
        List<Event> oldList = mEventsRepository.queryAllCurrentEventsSync();
        List<Event> newList = CalendarUtils.getCalendarEventsForToday(this);

        //first need to check if the 2 lists are the same or if different what type of update
        //needed
        HashMap<String, List<Event>> listsToAdd = CalendarUtils.compareCalendars(oldList, newList);
        List<Event> geofenceList = listsToAdd.get(Constants.LIST_NEEDS_FENCE_UPDATE);
        List<Event> noGeofenceList = listsToAdd.get(Constants.LIST_NO_FENCE_UPDATE);

        //remove all old events and insert all events that do not need new fences
        if (geofenceList.size() > 0) {
            //get the location saved to shared prefs, if it is a valid location
            //add the info from distanceMatrix and save the new fences
            Location location = getLocation();
            if (location != null) {
                setOrRemoveFences(geofenceList, location);
                //delete old events
                mEventsRepository.deleteAllEvents();
                //combine lists to make one upload
                geofenceList.addAll(noGeofenceList);
                mEventsRepository.insertAll(geofenceList);
                jobFinished(mJobParameters, false);
            } else {
                //if the location is not there or not valid try to get the location and
                //do the same work as before
                if (!SystemUtils.hasLocationPermissions(this)) return;
                mLocationProviderClient.getLastLocation().addOnSuccessListener(mAppExecutors.diskIO(), newLocation -> {
                    if (newLocation != null) {
                        mSharedPreferences.edit().putString(Constants.USER_LOCATION_PREFS_KEY, LocationUtils.locationToLatLngString(newLocation)).apply();
                    }
                    setOrRemoveFences(geofenceList, newLocation);
                    //delete old events
                    mEventsRepository.deleteAllEvents();
                    //combine lists to make one upload
                    geofenceList.addAll(noGeofenceList);
                    mEventsRepository.insertAll(geofenceList);
                    jobFinished(mJobParameters, false);
                });
            }
            // if there is no geofence list finish job
        } else if (noGeofenceList.size() > 0) {
            mEventsRepository.deleteAllEvents();
            mEventsRepository.insertAll(noGeofenceList);
            jobFinished(mJobParameters, false);
        } else {
            mEventsRepository.deleteAllEvents();
            mAppExecutors.mainThread().execute(() -> {
                MainActivity.setFinishedLoading(true);
            });
            jobFinished(mJobParameters, false);
        }

    }


    private Location getLocation() {
        Location location = null;
        if (mSharedPreferences.contains(Constants.USER_LOCATION_PREFS_KEY)) {
            String locationString = mSharedPreferences.getString(Constants.USER_LOCATION_PREFS_KEY, "");
            location = LocationUtils.locationFromLatLngString(locationString);
        }
        return location;
    }

    /**
     * add the distance data to the list and if all good update fences,
     * if unable to get data then remove the fences as no longer relevant,
     * NOTE - need to set distance data here even thought the creator does it in the
     * event that there is no location. That is only a fix if there is no location however
     * here it is needed because the event information changed
     *
     * @param eventsToAddWithGeofences
     * @param location
     */
    private void setOrRemoveFences(List<Event> eventsToAddWithGeofences, Location location) {
        boolean wasAdded;
        AwarenessFencesCreator fencesCreator = new AwarenessFencesCreator.Builder(null).build();
        wasAdded = DirectionsUtils.addDistanceInfoToEventList(eventsToAddWithGeofences, location);
        if (wasAdded) {
            fencesCreator.setEventList(eventsToAddWithGeofences);
            fencesCreator.buildAndSaveFences();
        } else fencesCreator.removeFences(eventsToAddWithGeofences.toArray(new Event[0]));
    }
}
