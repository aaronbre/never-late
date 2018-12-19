package com.aaronbrecher.neverlate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers.StartJobIntentServiceBroadcastReceiver;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.inject.Inject;

@WorkerThread
public class AwarenessFencesCreator{
    @Inject
    NeverLateApp mApp;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors mAppExecutors;

    private FenceClient mFenceClient;
    private List<Event> mEventList;
    private Location mLocation = null;
    private PendingIntent mPendingIntent;
    private long mAlertTime;

    public List<Event> getEventList() {
        return mEventList;
    }

    public void setEventList(List<Event> eventList) {
        mEventList = eventList;
    }

    private AwarenessFencesCreator(List<Event> eventList) {
        NeverLateApp.getApp().getAppComponent().inject(this);
        mFenceClient = Awareness.getFenceClient(mApp);
        mEventList = eventList;
        mAlertTime = getAlertTime();
    }

    private long getAlertTime() {
        String alertTime = mSharedPreferences.getString(Constants.ALERTS_PREFS_KEY, "");
        switch (alertTime) {
            case Constants.ALERT_TIME_SHORT:
                return Constants.TIME_FIVE_MINUTES;
            case Constants.ALERT_TIME_MEDIUM:
                return Constants.TIME_TEN_MINUTES;
            case Constants.ALERT_TIME_LONG:
                return Constants.TIME_FIFTEEN_MINUTES;
            default:
                return Constants.TIME_TEN_MINUTES;
        }
    }

    /**
     * Ideally the location will always be set by the Activity Recognition and AlarmService,
     * In edge case where it was not available then will try to update now
     */
    @SuppressLint("MissingPermission")
    @WorkerThread
    public void buildAndSaveFences() {
            mLocationProviderClient.getLastLocation().addOnSuccessListener(mAppExecutors.diskIO(), location -> {
                if (location == null) return;
                mLocation = location;
                //TODO this is probably not needed, as every call to this already added distance info
                // If the location is older then a day we can assume that distance info needs to be changed
                //this code should not be needed due to the activity recognition
                if(location.getTime() < System.currentTimeMillis() - Constants.ONE_DAY){
                    DirectionsUtils directionsUtils = new DirectionsUtils(mSharedPreferences, location);
                    directionsUtils.addDistanceInfoToEventList(mEventList);
                }
                mEventsRepository.insertAll(mEventList);
                updateFences();
            });
    }

    private List<AwarenessFenceWithName> createFences() {
        List<AwarenessFenceWithName> fenceList = new ArrayList<>();
        for (Event event : mEventList) {
            //added try/catch if creating a fence fails
            try {
                //all events will start with the value set to ROOM_INVALID... as a sentinal
                if (event.getDrivingTime() == Constants.ROOM_INVALID_LONG_VALUE) continue;
                String fenceName = Constants.AWARENESS_FENCE_MAIN_PREFIX + event.getId();
                long relevantTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
                long triggerTime = relevantTime - (event.getDrivingTime() * 1000);
                AwarenessFenceWithName fence = new AwarenessFenceWithName(createAwarenessFenceForEvent(triggerTime), fenceName);
                String arrivalFenceName = Constants.AWARENESS_FENCE_ARRIVAL_PREFIX + event.getId();
                AwarenessFenceWithName arrivalFence = new AwarenessFenceWithName(createArrivalFenceForEvent(event), arrivalFenceName);
                fenceList.add(arrivalFence);
                fenceList.add(fence);

                if(relevantTime == Converters.unixFromDateTime(event.getStartTime())){
                    fenceName = Constants.AWARENESS_FENCE_END_PREFIX + event.getId();
                    triggerTime = Converters.unixFromDateTime(event.getEndTime()) - (event.getDrivingTime() * 1000);
                    AwarenessFenceWithName endFence = new AwarenessFenceWithName(createAwarenessFenceForEvent(triggerTime), fenceName);
                    fenceList.add(endFence);
                }
            } catch (IllegalArgumentException | ConcurrentModificationException e) {
                //todo fix conccurent error
                e.printStackTrace();
            }
        }
        return fenceList;
    }

    /**
     * creates an awarenessFence it will have two triggers : Time and location, the triggerTime will be the time
     * of the event - the time it takes to drive there. The location will be the users current location which is
     * saved in the app class
     *
     * @param triggerTime the triggerTime will either be the event start or end
     * @return
     */
    private AwarenessFence createAwarenessFenceForEvent(long triggerTime) {
        if (ActivityCompat.checkSelfPermission(mApp, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        if(triggerTime < System.currentTimeMillis()) return null;
        AwarenessFence locationFence = LocationFence.in(mLocation.getLatitude(),
                mLocation.getLongitude(),
                Constants.LOCATION_FENCE_RADIUS,
                Constants.LOCATION_FENCE_DWELL_TIME);
        //TODO the end time is such for testing purposes in reality possibly will end the time before this
        AwarenessFence timeFence = TimeFence.inInterval(triggerTime - mAlertTime, triggerTime + Constants.TIME_FIFTEEN_MINUTES);
        return AwarenessFence.and(locationFence, timeFence);
    }

    /**
     * This will create an additional fence for each event for arrival at event,
     * this will be used to remove the event from tracking when the user makes it
     *
     * @param event the event to create fence for
     * @return
     */
    private AwarenessFence createArrivalFenceForEvent(Event event) {
        LatLng latLng = event.getLocationLatlng() != null ? event.getLocationLatlng()
                : LocationUtils.latlngFromAddress(mApp, event.getLocation());
        if (latLng == null) return null;

        long startTime = Converters.unixFromDateTime(event.getStartTime());
        long endTime = Converters.unixFromDateTime(event.getEndTime());
        if(startTime < System.currentTimeMillis() || endTime < System.currentTimeMillis()) return null;
        @SuppressLint("MissingPermission") AwarenessFence locationFence = LocationFence.in(latLng.latitude,
                latLng.longitude,
                Constants.ARRIVAL_FENCE_RADIUS,
                Constants.ARRIVAL_FENCE_DWELL_TIME);
        AwarenessFence timeFence = TimeFence.inInterval(startTime - Constants.TIME_TEN_MINUTES,
                endTime);
        return AwarenessFence.and(locationFence, timeFence);

    }

    private FenceUpdateRequest getUpdateRequest(List<AwarenessFenceWithName> fences) {
        FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();
        for (AwarenessFenceWithName fence : fences) {
            if (fence == null || fence.fence == null || fence.name == null || TextUtils.isEmpty(fence.name))
                continue;
            builder.addFence(fence.name, fence.fence, getPendingIntent());
        }
        return builder.build();
    }

    private PendingIntent getPendingIntent() {
        if (mPendingIntent != null) return mPendingIntent;
        Intent intent = new Intent(mApp, StartJobIntentServiceBroadcastReceiver.class);
        intent.setAction(Constants.ACTION_START_AWARENESS_FENCE_SERVICE);
        mPendingIntent = PendingIntent.getBroadcast(mApp,
                Constants.AWARENESS_TRANSITION_PENDING_INTENT_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }


    private void updateFences() {
        final List<AwarenessFenceWithName> fencelist = createFences();
        if (fencelist.size() == 0) return;
        FenceUpdateRequest request = getUpdateRequest(fencelist);
        if (request == null) return;
        mFenceClient.updateFences(request).addOnSuccessListener(aVoid -> {
            if (mApp.isInBackground()) return;
            if (fencelist.size() < mEventList.size())
                Toast.makeText(mApp, R.string.geofence_added_partial_success, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(mApp, R.string.geofence_added_success, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            if(mApp.isInBackground()) return;
            Toast.makeText(mApp, R.string.geofence_added_failed, Toast.LENGTH_SHORT).show();
        });

    }

    @WorkerThread
    public void removeFences(Event... events) {
        FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();
        for (Event event : events) {
            builder.removeFence(Constants.AWARENESS_FENCE_MAIN_PREFIX + event.getId());
            builder.removeFence(Constants.AWARENESS_FENCE_ARRIVAL_PREFIX + event.getId());
            builder.removeFence(Constants.AWARENESS_FENCE_END_PREFIX + event.getId());
        }
        mFenceClient.updateFences(builder.build());
    }

    public static class Builder {
        private List<Event> mEventList;

        public Builder(List<Event> eventList) {
            this.mEventList = eventList;
        }

        public AwarenessFencesCreator build() {
            return new AwarenessFencesCreator(mEventList);
        }
    }

    /**
     * This subclass is used to make it easier to add a geofence with a name
     * built from the Event. All names will have a prefix + the event ID
     */
    private class AwarenessFenceWithName {
        public AwarenessFence fence;
        public String name;

        public AwarenessFenceWithName(AwarenessFence fence, String name) {
            this.fence = fence;
            this.name = name;
        }
    }
}
