package com.aaronbrecher.neverlate.backgroundservices;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    LocationCallback mLocationCallback;

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
        CalendarUtils calendarUtils = new CalendarUtils(mSharedPreferences);
        List<Event> oldList = mEventsRepository.queryAllCurrentEventsSync();
        List<Event> newList = calendarUtils.getCalendarEventsForToday(this);
        //first need to check if the 2 lists are the same or if different what type of update
        //needed
        HashMap<String, List<Event>> listsToAdd = calendarUtils.compareCalendars(oldList, newList);
        List<Event> geofenceList = listsToAdd.get(Constants.LIST_NEEDS_FENCE_UPDATE);
        List<Event> noGeofenceList = listsToAdd.get(Constants.LIST_NO_FENCE_UPDATE);
        try{
            Location location = Tasks.await(mLocationProviderClient.getLastLocation());
            if(location == null || location.getTime() < System.currentTimeMillis() - Constants.FIVE_HOUR){
                //if the location is invalid need to update the fences for all lists
                getNewLocation(newList);
            } else if(geofenceList.size() > 0){
                setOrRemoveFences(geofenceList, location);
                //delete old events
                mEventsRepository.deleteAllEvents();
                //combine lists to make one upload
                geofenceList.addAll(noGeofenceList);
                mEventsRepository.insertAll(geofenceList);
                //if there was a change refresh the analytics
                //TODO when the bug is found fix this
                FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
                dispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(dispatcher));
                jobFinished(mJobParameters, false);
            } else if(noGeofenceList.size() > 0){
                mEventsRepository.deleteAllEvents();
                mEventsRepository.insertAll(noGeofenceList);
                jobFinished(mJobParameters, false);
            }else {
                mEventsRepository.deleteAllEvents();
                mAppExecutors.mainThread().execute(() -> MainActivity.setFinishedLoading(true));
                jobFinished(mJobParameters, false);
            }
        } catch (InterruptedException | ExecutionException e) {
            //notify user that fences where not updated
            e.printStackTrace();
            mEventsRepository.deleteAllEvents();
            mAppExecutors.mainThread().execute(() -> {
                MainActivity.setFinishedLoading(true);
            });
            jobFinished(mJobParameters, false);
        }
    }

    @SuppressLint("MissingPermission")
    private void getNewLocation(List<Event> newList) {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mAppExecutors.diskIO().execute(() -> {
                    Location location = locationResult.getLastLocation();
                    //if we have a new location then create fence for a all lists
                    Log.i(TAG, "onLocationResult: recieved");
                    setOrRemoveFences(newList, location);
                    //delete old events
                    mEventsRepository.deleteAllEvents();
                    mEventsRepository.insertAll(newList);
                    jobFinished(mJobParameters, false);
                });
            }
        };
        mLocationProviderClient.requestLocationUpdates(new LocationRequest().setInterval(10)
                        .setNumUpdates(1)
                        .setExpirationDuration(15000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                mLocationCallback, Looper.getMainLooper())
                .addOnCompleteListener(task -> {
                    MainActivity.setFinishedLoading(true);
                    jobFinished(mJobParameters, false);
                });
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
