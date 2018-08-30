package com.aaronbrecher.neverlate.backgroundservices;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.aaronbrecher.neverlate.BuildConfig;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.models.Event;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.maps.GeoApiContext;

import java.util.List;

import javax.inject.Inject;

import static com.aaronbrecher.neverlate.Utils.BackgroundUtils.DEFAULT_JOB_TIMEFRAME;

public class CalendarAlarmService extends IntentService {
    public static final String TAG = CalendarAlarmService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;

    GeoApiContext mGeoApiContext = new GeoApiContext().setApiKey(BuildConfig.GOOGLE_API_KEY);


    public CalendarAlarmService() {
        super("Calendar-alarm-service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
        Log.i(TAG, "onCreate: Alarm service was started");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent != null){
            String action = intent.getAction();
            if(action.equals(Constants.ACTION_ADD_CALENDAR_EVENTS)){
                mEventsRepository.deleteAllEvents();
                List<Event> eventList = CalendarUtils.getCalendarEventsForToday(this);
                mEventsRepository.insertAll(eventList);
                initializeGeofences(eventList);
                FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
                //TODO use user provided timeframe from preferences rather then default
                Job job = BackgroundUtils.createJob(dispatcher, DEFAULT_JOB_TIMEFRAME);
                dispatcher.mustSchedule(job);
            }
        }
    }

    private void getLocationAndInsertToDb(List<Event> events){

    }

    private void initializeGeofences(List<Event> eventList){
        Geofencing geofencing = Geofencing.builder(eventList);
        geofencing.createAndSaveGeofences();
    }


}