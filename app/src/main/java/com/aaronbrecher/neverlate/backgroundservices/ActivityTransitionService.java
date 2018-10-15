package com.aaronbrecher.neverlate.backgroundservices;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.JobIntentService;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.BuildConfig;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.GeoApiContext;

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

    static void enqueueWork(Context context, Intent work) {
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
            if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                stopDrivingForegroundService();
                setUpFences();
            } else if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                setUpDrivingService();
            }
        }
    }

    private void setUpDrivingService() {
        Intent intent = new Intent(this, DrivingForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopDrivingForegroundService(){
        Intent intent = new Intent(this, DrivingForegroundService.class);
        stopService(intent);
    }

    /**
     * If user has stopped driving assume he will remain in this location and update the
     * eventList with the distance info. Reset the AwarenessFences using the current
     * location information and current Location
     */
    private void setUpFences() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                mAppExecutors.diskIO().execute(() -> {
                    List<Event> eventList = mEventsRepository.queryAllCurrentEventsSync();
                    DirectionsUtils.addDistanceInfoToEventList(
                            new GeoApiContext().setApiKey(BuildConfig.GOOGLE_API_KEY),
                            eventList,
                            location);
                    AwarenessFencesCreator creator = new AwarenessFencesCreator.Builder(eventList).build();
                    creator.setEventList(eventList);
                    creator.buildAndSaveFences();
                    //save the new location to shared prefs
                    mSharedPreferences.edit()
                            .putString(Constants.USER_LOCATION_PREFS_KEY, LocationUtils.locationToLatLngString(location))
                            .apply();
                });
            }
        });
    }

}
