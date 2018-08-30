package com.aaronbrecher.neverlate.backgroundservices;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.aaronbrecher.neverlate.BuildConfig;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.interfaces.GeofencesUpdatedCallback;
import com.aaronbrecher.neverlate.interfaces.LocationCallback;
import com.aaronbrecher.neverlate.models.Event;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.GeoApiContext;

import java.util.List;

import javax.inject.Inject;

public class GeofenceJobService extends JobService implements GeofencesUpdatedCallback, LocationCallback {

    private static final String TAG = GeofenceJobService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    GeofencesRepository mGeofencesRepository;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;

    private GeoApiContext mGeoApiContext = new GeoApiContext().setApiKey(BuildConfig.GOOGLE_API_KEY);
    private JobParameters mJobParameters;
    private List<Event> mEventList;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: Jobservice was started");
        NeverLateApp.getApp().getAppComponent().inject(this);
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        this.mJobParameters = job;
        mHandlerThread = new HandlerThread("GeofenceJobServiceThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doWork();
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    /**
     * this job assumes that whenever there is a location saved for the event that should be
     * considered current. This will only work by using the activity recognition API to update the
     * location when user drives..
     * TODO (Rather this!!)either use activity recognition as stated or always load location here
     */
    private void doWork() {
        mEventList = mEventsRepository.queryAllCurrentEventsSync();
        if (isMissingLocations(mEventList)) {
            BackgroundUtils.getLocation(this, this, mLocationProviderClient);
        } else {
            addGeofences(mEventList);
        }
    }

    private boolean isMissingLocations(List<Event> events) {
        for (Event event : events) {
            if (event.getTimeTo() == Constants.ROOM_INVALID_LONG_VALUE || event.getDistance() == Constants.ROOM_INVALID_LONG_VALUE)
                return true;
        }
        return false;
    }

    private void addLocations(final List<Event> eventList) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                //need to create a new thread as the callback will execute after the doWork thread is finished
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DirectionsUtils.addLocationToEventList(mGeoApiContext, eventList, location);
                        mEventsRepository.insertAll(eventList);
                        addGeofences(eventList);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                addGeofences(eventList);
            }
        });
    }

    private void addGeofences(List<Event> eventList) {
        Geofencing geofencing = Geofencing.builder(eventList);
        geofencing.setCallback(this);
        geofencing.createAndSaveGeofences();
    }

    @Override
    public void geofencesUpdated(boolean wasUpdated) {
        mHandlerThread.quitSafely();
        jobFinished(mJobParameters, false);
    }

    @Override
    public void successCallback(final Location location) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                DirectionsUtils.addLocationToEventList(mGeoApiContext, mEventList, location);
                mEventsRepository.insertAll(mEventList);
                addGeofences(mEventList);
            }
        });
    }

    @Override
    public void failedCallback() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                addGeofences(mEventList);
            }
        });
    }
}
