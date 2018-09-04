package com.aaronbrecher.neverlate.backgroundservices;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.google.android.gms.location.Geofence;

import javax.inject.Inject;

public class AwarenessFenceTransitionService extends JobIntentService {
    @Inject
    EventsRepository mEventsRepository;

    public static final int JOB_ID = 1002;

    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, AwarenessFenceTransitionService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

    }

    /**
     * This will create a notification for the geofence by getting the event data
     * from Room using the Id of the geofence
     * TODO there is currently a bug when using notification to launch app parceable has issues no such issues when launching from intent in mainActivity...
     * @param geofence
     */
    private NotificationCompat.Builder createNotificationForFence(Geofence geofence, int transition) {
        String str = geofence.getRequestId().replaceAll("\\D+", "");
        int id = Integer.valueOf(str);
        Event event = mEventsRepository.queryEventById(id);
        //create the intent to be used to launch the detail screen of this event
        Intent intent;
        if (this.getResources().getBoolean(R.bool.is_tablet)) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, EventDetailActivity.class);
        }
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
        builder.setContentIntent(pendingIntent)
//                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            builder.setSmallIcon(R.drawable.ic_geofence_car)
                    .setContentTitle(getString(R.string.exiting_geofence_notification_title) + event.getLocation())
                    .setContentText(getString(R.string.exiting_geofence_notification_content) + event.getTitle())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(getString(R.string.exiting_geofence_notification_content) + event.getTitle()));

        } else {
            builder.setSmallIcon(R.drawable.ic_geofence_car)
                    .setContentTitle(getString(R.string.entering_geofence_notification_title) + event.getLocation())
                    .setContentText(getString(R.string.entering_geofence_notification_content) + event.getTitle())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(getString(R.string.entering_geofence_notification_content) + event.getTitle()));
        }
        return builder;
    }
}
