package com.aaronbrecher.neverlate.geofencing;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import javax.inject.Inject;

public class GeofenceTransitionsIntentService extends IntentService {
    private static final String TAG = GeofenceTransitionsIntentService.class.getSimpleName();
    private int mNotificationId = 0;

    @Inject
    EventsRepository mEventsRepository;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * Name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTransitionsIntentService() {
        super(GeofenceTransitionsIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((NeverLateApp)getApplication())
                .getAppComponent()
                .inject(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()){
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, "onHandleIntent: " + errorMessage);
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            List<Geofence> tiggeringGeofences = geofencingEvent.getTriggeringGeofences();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            for(Geofence geofence : tiggeringGeofences){
                NotificationCompat.Builder builder = createNotificationForFence(geofence);
                notificationManager.notify(mNotificationId, builder.build());
                mNotificationId++;
            }
        } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
            //TODO create notification for reentering the geofence
        }
    }

    /**
     * This will create a notification for the geofence by getting the event data
     * from Room using the Id of the geofence
     * @param geofence
     */
    private NotificationCompat.Builder createNotificationForFence(Geofence geofence) {
        String str = geofence.getRequestId().replaceAll("\\D+","");
        int id = Integer.valueOf(str);
        Event event = mEventsRepository.queryEventById(id);
        //create the intent to be used to launch the detail screen of this event
        Intent intent;
        if(this.getResources().getBoolean(R.bool.is_tablet)){
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, EventDetailActivity.class);
        }
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_geofence_car)
                .setContentTitle(getString(R.string.exiting_geofence_notification_title) + event.getLocation())
                .setContentText(getString(R.string.exiting_geofence_notification_content) + event.getTitle())
                .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(getString(R.string.exiting_geofence_notification_content) + event.getTitle()))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        return builder;
    }
}
