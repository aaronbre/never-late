package com.aaronbrecher.neverlate.backgroundservices;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.google.android.gms.awareness.fence.FenceState;

import javax.inject.Inject;

public class AwarenessFenceTransitionService extends JobIntentService {
    private static final String TAG = AwarenessFenceTransitionService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;

    private Event mEvent;

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
        FenceState fenceState = FenceState.extract(intent);
        if (fenceState.getFenceKey().contains(Constants.AWARENESS_FENCE_NAME_PREFIX)) {
            if (fenceState.getCurrentState() == FenceState.TRUE) {
                int id = Integer.valueOf(fenceState.getFenceKey().replaceAll("\\D+", ""));
                mEvent = mEventsRepository.queryEventById(id);
                NotificationCompat.Builder notificationBuilder = createNotificationForFence(mEvent);
                NotificationManagerCompat.from(this).notify(0, notificationBuilder.build());
            }
        }
    }

    /**
     * This will create a notification for the geofence by getting the event data
     * from Room using the Id of the geofence
     * TODO there is currently a bug when using notification to launch app parceable has issues no such issues when launching from intent in mainActivity...
     *
     * @param event
     */
    private NotificationCompat.Builder createNotificationForFence(Event event) {
        //create the intent to be used to launch the detail screen of this event
        Intent intent;
        if (this.getResources().getBoolean(R.bool.is_tablet)) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, EventDetailActivity.class);
        }
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String notificationText = getString(R.string.exiting_geofence_notification_content_1) + event.getTitle();
        if (event.getTimeTo() != null) {
            notificationText += " " + getString(R.string.exiting_geofence_notification_content_2)
                    + DirectionsUtils.readableTravelTime(event.getTimeTo());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
        builder.setContentIntent(pendingIntent)
//TODO only here for testing uncomment after                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setSmallIcon(R.drawable.ic_geofence_car)
                .setContentTitle(getString(R.string.exiting_geofence_notification_title) + event.getLocation())
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText));
        return builder;
    }
}
