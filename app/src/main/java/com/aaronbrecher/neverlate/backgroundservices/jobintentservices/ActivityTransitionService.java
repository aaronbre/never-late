package com.aaronbrecher.neverlate.backgroundservices.jobintentservices;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers.DrivingLocationUpdatesBroadcastReceiver;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;

import java.util.List;

import javax.inject.Inject;

public class ActivityTransitionService extends JobIntentService {
    static int JOB_ID = 1001;

    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors mAppExecutors;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent().inject(this);
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ActivityTransitionService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            //for our purposes only the last event should be considered, if user already parked previous data
            //is not relevant and vice versa
            List<ActivityTransitionEvent> events = result.getTransitionEvents();
            ActivityTransitionEvent event = events.get(events.size() - 1);
            //only execute code for the in-vehicle activity
            if (event.getActivityType() != DetectedActivity.IN_VEHICLE) return;

            if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                setUpFences();
                stopLocationUpdates();
            } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                requestLocationUpdates();
            }
        }
    }


    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (!SystemUtils.hasLocationPermissions(this)) return;
        mLocationProviderClient.requestLocationUpdates(createLocationRequest(), getPendingIntent());
    }

    private LocationRequest createLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval(Constants.TIME_FIVE_MINUTES)
                .setFastestInterval(Constants.TIME_FIVE_MINUTES)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return request;
    }


    private void stopLocationUpdates() {
        mLocationProviderClient.removeLocationUpdates(getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, DrivingLocationUpdatesBroadcastReceiver.class);
        intent.setAction(Constants.ACTION_PROCESS_LOCATION_UPDATE);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    /**
     * If user has stopped driving assume he will remain in this location and update the
     * eventList with the distance info. Reset the AwarenessFences using the current
     * location information and current Location
     */
    @SuppressLint("MissingPermission")
    private void setUpFences() {

        if (!SystemUtils.hasLocationPermissions(this)) {
            return;
        }

        mLocationProviderClient.getLastLocation().addOnSuccessListener(mAppExecutors.diskIO(), location ->{
            List<Event> eventList = mEventsRepository.queryAllCurrentEventsSync();
            DirectionsUtils.addDistanceInfoToEventList(eventList, location);
            AwarenessFencesCreator creator = new AwarenessFencesCreator.Builder(eventList).build();
            creator.setEventList(eventList);
            creator.buildAndSaveFences();
            //save the new location to shared prefs
            mSharedPreferences.edit()
                    .putString(Constants.USER_LOCATION_PREFS_KEY, LocationUtils.locationToLatLngString(location))
                    .apply();
        });
    }
}
