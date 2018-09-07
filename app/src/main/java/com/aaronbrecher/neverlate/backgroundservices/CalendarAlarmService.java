package com.aaronbrecher.neverlate.backgroundservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.aaronbrecher.neverlate.BuildConfig;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.interfaces.LocationCallback;
import com.aaronbrecher.neverlate.models.Event;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.gson.Gson;
import com.google.maps.GeoApiContext;

import java.util.List;

import javax.inject.Inject;

import static com.aaronbrecher.neverlate.Utils.BackgroundUtils.DEFAULT_JOB_TIMEFRAME;

public class CalendarAlarmService extends JobIntentService implements LocationCallback {
    //TODO alarm service was not triggered at midnight... investigate
    public static final String TAG = CalendarAlarmService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    NeverLateApp mApp;

    private List<Event> mEventList;
    private GeoApiContext mGeoApiContext = new GeoApiContext().setApiKey(BuildConfig.GOOGLE_API_KEY);
    static final int JOB_ID = 1000;

    public CalendarAlarmService() {
    }

    static void enqueueWork(Context context, Intent work) {
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
    }

    private void initializeGeofences(List<Event> eventList) {
        AwarenessFencesCreator geofenceCreator = new AwarenessFencesCreator.Builder(eventList).build();
        geofenceCreator.buildAndSaveFences();
    }


    @Override
    public void getLocationSuccessCallback(final Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(location != null){
                    Gson gson = new Gson();
                    mSharedPreferences.edit().putString(Constants.USER_LOCATION_PREFS_KEY, gson.toJson(location)).apply();
                    if(mEventList == null || mEventList.size() == 0) return;
                    DirectionsUtils.addDistanceInfoToEventList(mGeoApiContext, mEventList, location);
                    mEventsRepository.insertAll(mEventList);
                    initializeGeofences(mEventList);
                    initializeActivityRecognition();
                }
            }
        }, "CASlocationThread").start();
    }

    private void initializeActivityRecognition() {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job job = BackgroundUtils.setUpActivityRecognitionJob(dispatcher);
        dispatcher.mustSchedule(job);
    }

    @Override
    public void getLocationFailedCallback() {
        //TODO Remove this with find a different way to handle no location
    }


}
