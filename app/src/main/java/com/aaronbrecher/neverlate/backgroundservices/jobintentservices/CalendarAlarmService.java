package com.aaronbrecher.neverlate.backgroundservices.jobintentservices;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.interfaces.LocationCallback;
import com.aaronbrecher.neverlate.models.Event;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.List;

import javax.inject.Inject;

/**
 * Service to update the Database from the Native Calendar app, will be scheduled
 * for 12AM each day
 */
public class CalendarAlarmService extends JobIntentService implements LocationCallback {
    public static final String TAG = CalendarAlarmService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    NeverLateApp mApp;
    @Inject
    AppExecutors mAppExecutors;

    private List<Event> mEventList;
    static final int JOB_ID = 1000;

    public CalendarAlarmService() {
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, CalendarAlarmService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
        Log.i(TAG, "onCreate: Alarm service was started");
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        mEventsRepository.deleteAllEvents();
        mEventList = CalendarUtils.getCalendarEventsForToday(this);
        mEventsRepository.insertAll(mEventList);
        BackgroundUtils.getLocation(this, this, mLocationProviderClient);
        initializeActivityRecognition();
        initializeCalendarCheckJob();
    }

    /**
     * Set up Geofences for the provided list of events
     * @param eventList
     */
    private void initializeGeofences(List<Event> eventList) {
        AwarenessFencesCreator geofenceCreator = new AwarenessFencesCreator.Builder(eventList).build();
        geofenceCreator.buildAndSaveFences();
    }


    @Override
    public void getLocationSuccessCallback(final Location location) {
        mAppExecutors.diskIO().execute(() -> {
            if(location != null){
                mSharedPreferences.edit().putString(Constants.USER_LOCATION_PREFS_KEY, LocationUtils.locationToLatLngString(location)).apply();
                if(mEventList == null || mEventList.size() == 0) return;
                DirectionsUtils.addDistanceInfoToEventList(mEventList, location);
                mEventsRepository.insertAll(mEventList);
                initializeGeofences(mEventList);
            }
        });
    }

    /**
     * initialize the Activity monitor to track when user is in a vehicle
     */
    private void initializeActivityRecognition() {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job job = BackgroundUtils.setUpActivityRecognitionJob(dispatcher);
        dispatcher.mustSchedule(job);
    }

    /**
     * initialize a job to perodically check the calendar for changes throughout
     * the day
     */
    private void initializeCalendarCheckJob(){
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job job = BackgroundUtils.setUpPeriodicCalendarChecks(dispatcher);
        dispatcher.mustSchedule(job);
    }

    @Override
    public void getLocationFailedCallback() {
        //TODO Remove this with find a different way to handle no location
    }


}
