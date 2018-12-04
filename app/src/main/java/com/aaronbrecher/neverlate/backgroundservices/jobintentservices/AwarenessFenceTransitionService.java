package com.aaronbrecher.neverlate.backgroundservices.jobintentservices;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.google.android.gms.awareness.fence.FenceState;

import javax.inject.Inject;

/**
 * Service to be triggered when the user triggers the awarenessFence
 */
public class AwarenessFenceTransitionService extends JobIntentService {
    private static final String TAG = AwarenessFenceTransitionService.class.getSimpleName();
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    SharedPreferences mSharedPreferences;

    private Event mEvent;

    public static final int JOB_ID = 1002;

    public static void enqueueWork(Context context, Intent work) {
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
        if (fenceState.getFenceKey().contains(Constants.AWARENESS_FENCE_PREFIX)) {
            String fenceKey = fenceState.getFenceKey();
            int id = Integer.valueOf(fenceKey.replaceAll("\\D+", ""));
            mEvent = mEventsRepository.queryEventById(id);

            //if the event was deleted then need to remove the fence, or if the user is triggering the arrival fence
            //then the user has arrived at the event and need to remove it so it doesn't bother the user again.
            //TODO possibly give the user the option here to delete the event from the calendar
            if(mEvent == null || (fenceKey.contains(Constants.AWARENESS_FENCE_ARRIVAL_PREFIX) && fenceState.getCurrentState() == FenceState.TRUE)){
                mEvent = mEvent != null ? mEvent : new Event();
                mEvent.setId(id);
                AwarenessFencesCreator fencesCreator = new AwarenessFencesCreator.Builder(null).build();
                fencesCreator.removeFences(mEvent);
                return;
            }

            //if the fence is true then user is still in his location and needs to
            //leave to his event now.
            if ((fenceKey.contains(Constants.AWARENESS_FENCE_MAIN_PREFIX) || fenceKey.contains(Constants.AWARENESS_FENCE_END_PREFIX))
                    && fenceState.getCurrentState() == FenceState.TRUE) {
                //this is a sanity check in a case where the event was removed from the DB
                //but the fence was not removed
                if(mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) == Constants.ROOM_INVALID_LONG_VALUE){
                    NotificationCompat.Builder notificationBuilder = createNotificationForFence(mEvent);
                    NotificationManagerCompat.from(this).notify(mEvent.getId(), notificationBuilder.build());
                }

            }
        }
    }

    /**
     * This will create a notification for the geofence by getting the event data
     * from Room using the Id of the geofence
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
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson(event));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String notificationText;
        if (event.getDrivingTime() != Constants.ROOM_INVALID_LONG_VALUE) {
            notificationText = getString(R.string.exiting_geofence_notification_content_with_time,
                    event.getTitle(), DirectionsUtils.readableTravelTime(event.getDrivingTime()));
        }else {
            notificationText = getString(R.string.exiting_geofence_notification_content, event.getTitle());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
        builder.setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setSmallIcon(R.drawable.ic_geofence_car)
                .setContentTitle(getString(R.string.exiting_geofence_notification_title) + event.getLocation())
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText));
        return builder;
    }
}
