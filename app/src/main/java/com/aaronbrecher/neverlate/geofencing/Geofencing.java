package com.aaronbrecher.neverlate.geofencing;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

import javax.inject.Inject;


public class Geofencing {
    private static final String TAG = Geofencing.class.getSimpleName();

    private Context mContext;
    private float mMilesPerMinute;
    private List<Event> mEvents;
    private List<Geofence> mGeofenceList;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;

    @Inject
    public SharedPreferences mSharedPreferences;

    public Geofencing(Context context, List<Event> events) {
        mContext = context;
        mEvents = events;
        mGeofencingClient = LocationServices.getGeofencingClient(mContext);
        mMilesPerMinute = mSharedPreferences.getFloat(Constants.MILES_PER_MINUTE_PREFS_KEY, .5f);
    }

    /**
     * Set up a geofence for all events that contain a location and add all
     * the fences to the system
     */
    public void setUpGeofences() {
        for (Event event : mEvents) {

            if (event.getLocation() == null || event.getLocation().equals("")) {
                continue;
            }
            addGeofence(event);
        }
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mContext, R.string.geofence_added_success,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, R.string.geofence_added_failed,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * adds a new Geofence for each event, calculates the fence according to time left
     * to event and the distance multiplier to determine minutes per mile this will be taken
     * from shared preferences
     *
     * @param event the event to set up a fence for
     */
    private void addGeofence(Event event) {
        int fenceRadius = getFenceRadius(event.getStartTime());
        LatLng latLng = LocationUtils.latlngFromAddress(mContext, event.getLocation());
        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(String.valueOf(event.getId()))
                .setCircularRegion(latLng.latitude, latLng.longitude, fenceRadius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(event.getEndTime())
                .build()
        );
    }

    /**
     * Get the fence Radius based on the time and the milesPerMinute Multiplier
     *
     * @param startTime the start time for the event
     * @return an integer value of the rounded radius in meters
     */
    private int getFenceRadius(long startTime) {
        double minutesToEvent = (startTime - System.currentTimeMillis()) / 60000.0;
        double milesRadius = minutesToEvent * mMilesPerMinute;
        double meterRadius = (milesRadius * 1.609) * 1000;
        Log.i(TAG, "getFenceRadius: " + Math.round(meterRadius));
        return (int) Math.round(meterRadius);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(){
        if(mGeofencePendingIntent != null){
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceTransitionsIntentService.class);
        mGeofencePendingIntent = PendingIntent.getService(mContext,
                Constants.GEOFENCE_TRANSITION_PENDING_INTENT_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }


    public void setEvents(List<Event> events) {
        mEvents = events;
    }
}
