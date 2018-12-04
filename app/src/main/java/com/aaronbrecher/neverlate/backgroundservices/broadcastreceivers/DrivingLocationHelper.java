package com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class DrivingLocationHelper {
    private static final int EVENT_TIME = 1;
    private static final int BUFFER_TIME = 2;
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors mAppExecutors;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mFusedLocationProviderClient;

    private Location mLocation;
    private Context mContext;
    private double mDefaultSpeed;
    private List<Event> mEvents;

    @Inject
    DrivingLocationHelper(Location location, Context context) {
        NeverLateApp.getApp().getAppComponent().inject(this);
        this.mLocation = location;
        this.mContext = context;
        String speedString = mSharedPreferences.getString(context.getString(R.string.prefs_speed_key), "");
        try{
            mDefaultSpeed = Double.parseDouble(speedString);
        } catch (NumberFormatException e){
            mDefaultSpeed = .5;
        }
    }

    @SuppressLint("MissingPermission")
    public void checkAllEvents(){
        if(!SystemUtils.hasLocationPermissions(mContext)) return;
        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(mAppExecutors.diskIO(), location -> {
            mEvents = mEventsRepository.queryAllCurrentEventsSync();
            if(mEvents == null) return;
            mLocation = location;
            for(Event event : mEvents){
                if(alreadyNotified(String.valueOf(event.getId()))) continue;
                int distanceToEvent = determineDistanceToEvent(event, mLocation);
                //if the user is within 100 meters we can assume he made the event
                if(distanceToEvent > 100){
                    long drivingTimeToEventMillis = getDrivingTimeToEventMillis(event, (double) distanceToEvent);
                    //deterimine time of arrival to location
                    long arrivalTime = System.currentTimeMillis() + drivingTimeToEventMillis;
                    long eventTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
                    long bufferTime = eventTime - Constants.TIME_FIFTEEN_MINUTES;
                    if (arrivalTime >= eventTime) {
                        Boolean pastEndTime = eventTime == Converters.unixFromDateTime(event.getEndTime());
                        if(mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) == Constants.ROOM_INVALID_LONG_VALUE){
                            showEventNotification(event, EVENT_TIME, pastEndTime);
                        }
                    } else if (arrivalTime >= bufferTime) {
                        showEventNotification(event, BUFFER_TIME, null);
                    }
                }
                else {
                    //do something for user made it to event?
                }
            }
        });
    }

    //determine driving speed to event based on the data from the DistanceMatrix, will use
    //the distance/driving time to get a decent representation, otherwise will assume an average speed
    private long getDrivingTimeToEventMillis(Event event, double distanceToEvent) {
        double drivingTime = event.getDrivingTime().doubleValue();
        double distance = event.getDistance().doubleValue();
        double speed = drivingTime > 0 && distance > 0 ?
                (distance / 1000) / (drivingTime / 60) : mDefaultSpeed;

        return (long) (distanceToEvent / 1000 / speed) * 60 * 1000;
    }

    /**
     * determine the current distance to the event, this will be done using the location.distance method.
     * This will be an "as the crow flies" distance which is not as accurate as DistanceMatrix data but
     * making an API call for each event each time is not financially possible...
     *
     * @param event    current event
     * @param location the users current location as of triggering the location request
     * @return
     */
    private int determineDistanceToEvent(Event event, Location location) {
        //sanity check in case the event does not have a latlng, in that case will try to
        //get it here if not don't track
        LatLng eventLatlng = event.getLocationLatlng() != null ? event.getLocationLatlng()
                : LocationUtils.latlngFromAddress(mContext, event.getLocation());
        if(eventLatlng == null) return -1;
        Location eventLocation = new Location("");
        eventLocation.setLatitude(event.getLocationLatlng().latitude);
        eventLocation.setLongitude(event.getLocationLatlng().longitude);
        return (int) location.distanceTo(eventLocation);
    }

    private void showEventNotification(Event event, int type, Boolean pastEndTime) {
        if(alreadyNotified(String.valueOf(event.getId()))) return;
        Intent intent = new Intent(mContext, EventDetailActivity.class);
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson(event));
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_geofence_car)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (type == EVENT_TIME) {
            builder.setContentTitle(mContext.getString(R.string.driving_notification_title_missed));
            if (pastEndTime) {
                builder.setContentText(mContext.getString(R.string.driving_notification_content_missed_end))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.driving_notification_content_missed_end)));
            } else {
                builder.setContentText(mContext.getString(R.string.driving_notification_content_missed_start))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.driving_notification_content_missed_start)));
            }

        } else {
            builder.setContentTitle(mContext.getString(R.string.driving_nofication_time_to_go_title))
                    .setContentText(mContext.getString(R.string.driving_nofication_time_to_go_content))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.driving_nofication_time_to_go_content)));
        }
        NotificationManagerCompat.from(mContext).notify(0, builder.build());
    }

    private boolean alreadyNotified(String id){
        Set<String> set = mSharedPreferences.getStringSet(Constants.DISABLED_DRIVING_EVENTS, new HashSet<>());
        if(set.contains(id)) return true;
        set.add(id);
        mSharedPreferences.edit().putStringSet(Constants.DISABLED_DRIVING_EVENTS, set).apply();
        return false;
    }

}
