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
import com.aaronbrecher.neverlate.Utils.MapUtils;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Utility class used to set up geofences, for now the distance of the fence is set up
 * in a naive way assuming a set amount of time to travel a mile. The user can change
 * the preferred amount of time per mile.
 * TODO create a paid version which will get the time per mile in a more ideal way,
 * possibly using Google Directions API to get distances from numerous points and
 * average out the time
 */
public class Geofencing {
    private static final String TAG = Geofencing.class.getSimpleName();

    private Context mContext;
    private float mMilesPerMinute;
    private List<Event> mEvents;
    private List<Geofence> mGeofenceList;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    public SharedPreferences mSharedPreferences;

    public Geofencing(Context context, List<Event> events, SharedPreferences sharedPreferences) {
        mContext = context;
        mEvents = events;
        mGeofencingClient = LocationServices.getGeofencingClient(mContext);
        this.mSharedPreferences = sharedPreferences;
        mMilesPerMinute = mSharedPreferences.getFloat(Constants.MILES_PER_MINUTE_PREFS_KEY, .5f);
        mGeofenceList = new ArrayList<>();
    }

    /**
     * Set up a geofence for all events that contain a location and add all
     * the fences to the system
     *
     * @return a list of all the geofences to be added to the DB
     */
    public List<GeofenceModel> setUpGeofences() {
        List<GeofenceModel> fenceList = new ArrayList<>();
        for (Event event : mEvents) {
            //add a Geofence to the DB and to the Geofence List to be added to system
            fenceList.add(addGeofence(event));
        }
        GeofencingRequest request = getGeofencingRequest();
        if (request != null && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
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

        return fenceList;
    }

    /**
     * adds a new Geofence for each event, calculates the fence according to time left
     * to event and the distance multiplier to determine minutes per mile this will be taken
     * from shared preferences
     *
     * @param event the event to set up a fence for
     * @return GeofenceModel a representation of the fence to add to the DB
     */
    private GeofenceModel addGeofence(Event event) {
        int fenceRadius = MapUtils.getFenceRadius(MapUtils.determineRelevantTime(event.getStartTime(), event.getEndTime()), mMilesPerMinute);
        LatLng latLng = LocationUtils.latlngFromAddress(mContext, event.getLocation());
        String requestId = Constants.GEOFENCE_REQUEST_ID + event.getId();
        Geofence geofence = new Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(latLng.latitude, latLng.longitude, fenceRadius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Converters.unixFromDateTime(event.getEndTime()) - System.currentTimeMillis())
                .setNotificationResponsiveness(Constants.GEOFENCE_RESPONSE_MILLIS)
                .build();
        mGeofenceList.add(geofence);
        return new GeofenceModel(requestId, fenceRadius);
    }



    private GeofencingRequest getGeofencingRequest() {
        if(mGeofenceList.size() == 0) return null;
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
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
