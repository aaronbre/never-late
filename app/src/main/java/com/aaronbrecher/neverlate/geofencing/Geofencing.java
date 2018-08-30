package com.aaronbrecher.neverlate.geofencing;

import android.Manifest;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.dependencyinjection.AppModule;
import com.aaronbrecher.neverlate.dependencyinjection.DaggerGeofencingComponent;
import com.aaronbrecher.neverlate.dependencyinjection.RoomModule;
import com.aaronbrecher.neverlate.interfaces.GeofencesUpdatedCallback;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.errors.ApiException;

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

    private Application mContext;

    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    GeofencesRepository mGeofencesRepository;
    @Inject
    GeofencingClient mGeofencingClient;

    private double mKmPerMinute;
    private List<Event> mEvents;
    private List<Geofence> mGeofenceList;
    private List<GeofenceModel> mGeofenceModels;
    private PendingIntent mGeofencePendingIntent;
    private GeofencesUpdatedCallback mCallback;



    @Inject
    public Geofencing() {
        mContext = NeverLateApp.getApp();
        DaggerGeofencingComponent.builder()
                .appModule(new AppModule((NeverLateApp) mContext))
                .roomModule(new RoomModule())
                .build()
                .inject(this);

        mGeofencingClient = LocationServices.getGeofencingClient(mContext);
        mKmPerMinute = mSharedPreferences.getFloat(Constants.KM_PER_MINUTE_PREFS_KEY, 1);
        mGeofenceList = new ArrayList<>();
        mGeofenceModels = new ArrayList<>();

    }

    /**
     * Set up a geofence for all events that contain a location and add all
     * the fences to the system
     *
     * @return a list of all the geofences to be added to the DB
     */
    public void createAndSaveGeofences() {
        for (Event event : mEvents) {
            addGeofenceToRequestAndModel(event);
        }
        GeofencingRequest request = getGeofencingRequest();
        if (request != null && ActivityCompat.checkSelfPermission(
                mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    mGeofencesRepository.insertAll(mGeofenceModels);
                                    if (mCallback != null) mCallback.geofencesUpdated(true);
                                }
                            }).start();
                            Toast.makeText(mContext, R.string.geofence_added_success,
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            determineErrorMessage(e);
                            Log.e(TAG, "Geofence-onFailure: ", e);
                            if (mCallback != null) mCallback.geofencesUpdated(false);
                            Toast.makeText(mContext, R.string.geofence_added_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    //TODO check why there is an error and react accordingly if error is 1000 geofences are not available
    //either bec location is off or device does not support...
    private String determineErrorMessage(Exception e) {
        if (e instanceof ApiException) {
        }
        return null;
    }

    /**
     * adds a new Geofence for each event, calculates the fence according to time left
     * to event and the distance multiplier to determine minutes per mile this will be taken
     * from shared preferences.
     * if available uses the distance and timeTo from the event
     * to create a more reliable radius
     *
     * @param event the event to set up a fence for
     */
    private void addGeofenceToRequestAndModel(Event event) {
        int fenceRadius;
        long relevantTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
        fenceRadius = GeofenceUtils.getFenceRadius(relevantTime, mKmPerMinute);
        LatLng latLng = event.getLocationLatlng();
        String requestId = Constants.GEOFENCE_REQUEST_ID + event.getId();
        Geofence geofence = new Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(latLng.latitude, latLng.longitude, fenceRadius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Converters.unixFromDateTime(event.getEndTime()) - System.currentTimeMillis())
                .setNotificationResponsiveness(Constants.GEOFENCE_RESPONSE_MILLIS)
                .build();
        mGeofenceList.add(geofence);
        mGeofenceModels.add(new GeofenceModel(requestId, fenceRadius));
    }


    private GeofencingRequest getGeofencingRequest() {
        if (mGeofenceList.size() == 0) return null;
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
        double speed = GeofenceUtils.getAverageSpeed(events);
        if (speed != 0) mKmPerMinute = speed;
    }

    public void setCallback(GeofencesUpdatedCallback callback) {
        this.mCallback = callback;
    }

    public static Geofencing builder(List<Event> events) {
        Geofencing geofencing = new Geofencing();
        geofencing.setEvents(events);
        return geofencing;
    }
}
