package com.aaronbrecher.neverlate.backgroundservices.jobintentservices

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.NonNull
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.Utils.DirectionsUtils
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.AwarenessFencesCreator
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity
import com.aaronbrecher.neverlate.ui.activities.MainActivity
import com.google.android.gms.awareness.fence.FenceState

import javax.inject.Inject

/**
 * Service to be triggered when the user triggers the awarenessFence
 */
class AwarenessFenceTransitionService : JobIntentService() {
    @Inject
    internal lateinit var mEventsRepository: EventsRepository
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences

    private var mEvent: Event? = null

    override fun onCreate() {
        super.onCreate()
        NeverLateApp.app.appComponent
                .inject(this)
    }

    override fun onHandleWork(@NonNull intent: Intent) {
        val fenceState = FenceState.extract(intent)
        if (fenceState.fenceKey.contains(Constants.AWARENESS_FENCE_PREFIX)) {
            val fenceKey = fenceState.fenceKey
            val id = Integer.valueOf(fenceKey.replace("\\D+".toRegex(), ""))
            mEvent = mEventsRepository.queryEventById(id)

            //if the event was deleted then need to remove the fence, or if the user is triggering the arrival fence
            //then the user has arrived at the event and need to remove it so it doesn't bother the user again.
            //TODO possibly give the user the option here to delete the event from the calendar
            if (mEvent == null || fenceKey.contains(Constants.AWARENESS_FENCE_ARRIVAL_PREFIX) && fenceState.currentState == FenceState.TRUE) {
                mEvent = if (mEvent != null) mEvent else Event()
                mEvent!!.id = id
                val fencesCreator = AwarenessFencesCreator.Builder(null).build()
                fencesCreator.removeFences(mEvent!!)
                return
            }

            //if the fence is true then user is still in his location and needs to
            //leave to his event now.
            if ((fenceKey.contains(Constants.AWARENESS_FENCE_MAIN_PREFIX) || fenceKey.contains(Constants.AWARENESS_FENCE_END_PREFIX)) && fenceState.currentState == FenceState.TRUE) {
                //this is a sanity check in a case where the event was removed from the DB
                //but the fence was not removed
                if (mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) == Constants.ROOM_INVALID_LONG_VALUE) {
                    val notificationBuilder = createNotificationForFence(mEvent!!)
                    NotificationManagerCompat.from(this).notify(mEvent!!.id, notificationBuilder.build())
                }

            }
        }
    }

    /**
     * This will create a notification for the geofence by getting the event data
     * from Room using the Id of the geofence
     * @param event
     */
    private fun createNotificationForFence(event: Event): NotificationCompat.Builder {
        //create the intent to be used to launch the detail screen of this event
        val intent: Intent
        if (this.resources.getBoolean(R.bool.is_tablet)) {
            intent = Intent(this, MainActivity::class.java)
        } else {
            intent = Intent(this, EventDetailActivity::class.java)
        }
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson(event))
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationText: String
        val leaveTime = mSharedPreferences.getString(getString(R.string.prefs_alerts_key), "10")
        notificationText = if (event.drivingTime != Constants.ROOM_INVALID_LONG_VALUE) {
            getString(R.string.exiting_geofence_notification_content_with_time,
                    leaveTime, event.title, DirectionsUtils.readableTravelTime(event.drivingTime!!))
        } else {
            getString(R.string.exiting_geofence_notification_content, leaveTime, event.title)
        }

        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
        builder.setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setSmallIcon(R.drawable.ic_geofence_car)
                .setContentTitle(getString(R.string.exiting_geofence_notification_title) + event.location!!)
                .setContentText(notificationText)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(notificationText))
        return builder
    }

    companion object {
        private val TAG = AwarenessFenceTransitionService::class.java.simpleName

        val JOB_ID = 1002

        fun enqueueWork(context: Context, work: Intent) {
            JobIntentService.enqueueWork(context, AwarenessFenceTransitionService::class.java, JOB_ID, work)
        }
    }
}