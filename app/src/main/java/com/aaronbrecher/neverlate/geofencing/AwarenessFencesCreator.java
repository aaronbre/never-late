package com.aaronbrecher.neverlate.geofencing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.backgroundservices.StartJobIntentServiceBroadcastReceiver;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.dependencyinjection.AppModule;
import com.aaronbrecher.neverlate.dependencyinjection.DaggerGeofencingComponent;
import com.aaronbrecher.neverlate.dependencyinjection.RoomModule;
import com.aaronbrecher.neverlate.interfaces.LocationCallback;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.fence.TimeFence;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AwarenessFencesCreator implements LocationCallback {
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

    public List<Event> getEventList() {
        return mEventList;
    }

    public void setEventList(List<Event> eventList) {
        mEventList = eventList;
    }

    private AwarenessFencesCreator(List<Event> eventList) {
        DaggerGeofencingComponent.builder()
                .appModule(new AppModule(NeverLateApp.getApp()))
                .roomModule(new RoomModule())
                .build()
                .inject(this);

        if (mSharedPreferences.contains(Constants.USER_LOCATION_PREFS_KEY)) {
            String loc = mSharedPreferences.getString(Constants.USER_LOCATION_PREFS_KEY, "");
            mLocation = LocationUtils.locationFromLatLngString(loc);
        }
        mFenceClient = Awareness.getFenceClient(mApp);
        mEventList = eventList;
    }

    /**
     * Ideally the location will always be set by the Activity Recognition and AlarmService,
     * In edge case where it was not available then will try to update now
     */
    public void buildAndSaveFences() {
        if (mLocation == null) {
            BackgroundUtils.getLocation(this, mApp, mLocationProviderClient);
        } else {
            updateFences();
        }
    }

    private List<AwarenessFenceWithName> createFences() {
        List<AwarenessFenceWithName> fenceList = new ArrayList<>();
        for (Event event : mEventList) {
            //all events will start with the value set to ROOM_INVALID... as a sentinal
            if(event.getTimeTo() == Constants.ROOM_INVALID_LONG_VALUE) continue;
            String name = Constants.AWARENESS_FENCE_NAME_PREFIX + event.getId();
            long relevantTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
            long triggerTime = relevantTime - (event.getTimeTo() * 1000);
            AwarenessFenceWithName fence = new AwarenessFenceWithName(createAwarenessFenceForEvent(triggerTime), name);
            fenceList.add(fence);
        }
        return fenceList;
    }

    /**
     * creates an awarenessFence it will have two triggers : Time and location, the triggerTime will be the time
     * of the event - the time it takes to drive there. The location will be the users current location which is
     * saved in the app class
     *
     * @param triggerTime
     * @return
     */
    private AwarenessFence createAwarenessFenceForEvent(long triggerTime) {
        if (ActivityCompat.checkSelfPermission(mApp, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        AwarenessFence locationFence = LocationFence.in(mLocation.getLatitude(),
                mLocation.getLongitude(),
                Constants.LOCATION_FENCE_RADIUS,
                Constants.LOCATION_FENCE_DWELL_TIME);
        //TODO the end time is such for testing purposes in reality possibly will end the time before this
        AwarenessFence timeFence = TimeFence.inInterval(triggerTime - Constants.TIME_TEN_MINUTES, triggerTime + Constants.TIME_FIFTEEN_MINUTES);
        return AwarenessFence.and(locationFence, timeFence);
    }

    private FenceUpdateRequest getUpdateRequest(List<AwarenessFenceWithName> fences) {
        FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();
        for (AwarenessFenceWithName fence : fences) {
            if (fence == null) continue;
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
        mAppExecutors.networkIO().execute(()->{
            final List<AwarenessFenceWithName> fencelist = createFences();
            if(fencelist.size() == 0) return;
            FenceUpdateRequest request = getUpdateRequest(fencelist);
            mFenceClient.updateFences(request).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(mApp, R.string.geofence_added_success, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(mApp, R.string.geofence_added_failed, Toast.LENGTH_SHORT).show();
                //reschedule job
            });
        });
    }

    public void removeFences(Event... events){
        FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();
        for (Event event : events){
            builder.removeFence(Constants.AWARENESS_FENCE_NAME_PREFIX + event.getId());
        }
        mFenceClient.updateFences(builder.build());
    }

    @Override
    public void getLocationSuccessCallback(final Location location) {
        mAppExecutors.diskIO().execute(() -> {
            //if location was not saved need to update distance and time to event
            mSharedPreferences.edit()
                    .putString(Constants.USER_LOCATION_PREFS_KEY, LocationUtils.locationToLatLngString(location))
                    .apply();
            DirectionsUtils.addDistanceInfoToEventList(mEventList, location);
            mEventsRepository.insertAll(mEventList);
            updateFences();
        });
    }

    @Override
    public void getLocationFailedCallback() {
        //TODO reschedule the fences via a jobservice
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
