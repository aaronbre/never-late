package com.aaronbrecher.neverlate.backgroundservices;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.List;

import javax.inject.Inject;

public class DrivingForegroundService extends Service {
    public static final int SERVICE_ID = 101;
    public static final int EVENT_TIME = 1;
    public static final int BUFFER_TIME = 2;
    private static final String TAG = DrivingForegroundService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    AppExecutors mAppExecutors;

    private PendingIntent mCancelIntent;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private List<Event> mEventList;
    private double mDefaultSpeed;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent().inject(this);
        mDefaultSpeed = mSharedPreferences.getFloat(Constants.KM_PER_MINUTE_PREFS_KEY, .5f);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION_CANCEL_DRIVING_SERVICE)) {
            mLocationProviderClient.removeLocationUpdates(mCancelIntent);
            stopForeground(true);
            stopSelf();
        } else {
            Intent i = new Intent(this, DrivingForegroundService.class);
            intent.setAction(Constants.ACTION_CANCEL_DRIVING_SERVICE);
            mCancelIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            startForeground(SERVICE_ID, getForegroundNotification());
            mAppExecutors.diskIO().execute(() -> {
                mEventList = mEventsRepository.queryAllCurrentEventsSync();
                createLocationRequest();
                createLocationCallback();
                if (ActivityCompat.checkSelfPermission(DrivingForegroundService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(DrivingForegroundService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            });
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification getForegroundNotification() {
        return new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_geofence_car)
                .setContentTitle(getString(R.string.driving_foreground_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.driving_foreground_notification_text)))
                .addAction(R.drawable.ic_cancel_black_24dp, "Cancel tracking", mCancelIntent)
                .build();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.TIME_TEN_MINUTES)
                .setFastestInterval(Constants.TIME_TEN_MINUTES)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                for (Event event : mEventList) {
                    int distance = determineDistanceToEvent(event, location);
                    if (distance != -1) {
                        //determine speed from timeTo and distance
                        double speed = event.getTimeTo() != null && event.getDistance() != null ? (event.getTimeTo() / 60) / (event.getDistance() / 1000) : mDefaultSpeed;
                        //calculate time for current distance
                        int drivingTimeToEventMillis = (int) (distance / 1000 * speed) * 60 * 1000;
                        //deterimine time of arrival to location
                        long arrivalTime = System.currentTimeMillis() + drivingTimeToEventMillis;
                        long eventTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
                        long bufferTime = eventTime - Constants.TIME_FIFTEEN_MINUTES;
                        if (arrivalTime >= eventTime) {
                            Boolean pastEndTime = eventTime == Converters.unixFromDateTime(event.getEndTime());
                            showEventNotification(event, EVENT_TIME, pastEndTime);
                        } else if (arrivalTime >= bufferTime) {
                            showEventNotification(event, BUFFER_TIME, null);
                        }
                        //if under time for x amount minutes display notification
                    }
                }
            }
        };
    }

    private int determineDistanceToEvent(Event event, Location location) {
        // first check if user is still within the previous fence. If so no need to do extra work
        String latLngString = mSharedPreferences.getString(Constants.USER_LOCATION_PREFS_KEY, "");
        if (!latLngString.equals("")) {
            Location previousLocation = LocationUtils.locationFromLatLngString(latLngString);
            if (previousLocation.distanceTo(location) < Constants.LOCATION_FENCE_RADIUS/2)
                return -1;
        }

        Location eventLocation = new Location("");
        eventLocation.setLatitude(event.getLocationLatlng().latitude);
        eventLocation.setLongitude(event.getLocationLatlng().longitude);
        return (int) location.distanceTo(eventLocation);
    }

    private void showEventNotification(Event event, int type, Boolean pastEndTime) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_geofence_car)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (type == EVENT_TIME) {
            builder.setContentTitle(getString(R.string.driving_notification_title_missed));
            if (pastEndTime) {
                builder.setContentText(getString(R.string.driving_notification_content_missed_end))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.driving_notification_content_missed_end)));
            } else {
                builder.setContentText(getString(R.string.driving_notification_content_missed_start))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.driving_notification_content_missed_start)));
            }

        } else {
            builder.setContentTitle(getString(R.string.driving_nofication_time_to_go_title))
                    .setContentText(getString(R.string.driving_nofication_time_to_go_content))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.driving_nofication_time_to_go_content)));
        }
        NotificationManagerCompat.from(this).notify(0, builder.build());
    }

    @Override
    public boolean stopService(Intent name) {
        mLocationProviderClient.removeLocationUpdates(mLocationCallback);
        return super.stopService(name);
    }
}
