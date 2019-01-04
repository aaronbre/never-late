package com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.utils.GeofenceUtils
import com.aaronbrecher.neverlate.utils.LocationUtils
import com.aaronbrecher.neverlate.utils.SystemUtils
import com.aaronbrecher.neverlate.database.Converters
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnSuccessListener

import java.util.HashSet

import javax.inject.Inject

private const val EVENT_TIME = 1
private const val BUFFER_TIME = 2

class DrivingLocationHelper @Inject
internal constructor(private var mLocation: Location?, private val mContext: Context) {
    @Inject
    internal lateinit var mEventsRepository: EventsRepository
    @Inject
    internal lateinit var mAppExecutors: AppExecutors
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences
    @Inject
    internal lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private var mDefaultSpeed: Double = 0.toDouble()
    private var mEvents: List<Event>? = null

    init {
        NeverLateApp.app.appComponent.inject(this)
        val speedString = mSharedPreferences.getString(mContext.getString(R.string.prefs_speed_key), "")!!
        mDefaultSpeed = try {
            java.lang.Double.parseDouble(speedString)
        } catch (e: NumberFormatException) {
            .5
        }

    }

    @SuppressLint("MissingPermission")
    fun checkAllEvents() {
        if (!SystemUtils.hasLocationPermissions(mContext)) return

        mFusedLocationProviderClient.lastLocation.addOnSuccessListener(mAppExecutors.diskIO(), OnSuccessListener { location ->
            mEvents = mEventsRepository.queryAllCurrentEventsSync()
            mEvents?.let {
                mLocation = location
                for (event in it) {
                    if (alreadyNotified(event.id.toString())) continue
                    val distanceToEvent = determineDistanceToEvent(event, mLocation)
                    //if the user is within 100 meters we can assume he made the event
                    if (distanceToEvent > 100) {
                        val drivingTimeToEventMillis = getDrivingTimeToEventMillis(event, distanceToEvent.toDouble())
                        //deterimine time of arrival to location
                        val arrivalTime = System.currentTimeMillis() + drivingTimeToEventMillis
                        val eventTime = GeofenceUtils.determineRelevantTime(event.startTime, event.endTime)
                        val bufferTime = eventTime - Constants.TIME_FIFTEEN_MINUTES
                        if (arrivalTime >= eventTime) {
                            val pastEndTime = eventTime == Converters.unixFromDateTime(event.endTime)
                            if (mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) == Constants.ROOM_INVALID_LONG_VALUE) {
                                showEventNotification(event, EVENT_TIME, pastEndTime)
                            }
                        } else if (arrivalTime >= bufferTime) {
                            showEventNotification(event, BUFFER_TIME, null)
                        }
                    } else {
                        //do something for user made it to event?
                    }
                }
            }

        })
    }

    //determine driving speed to event based on the data from the DistanceMatrix, will use
    //the distance/driving time to get a decent representation, otherwise will assume an average speed
    private fun getDrivingTimeToEventMillis(event: Event, distanceToEvent: Double): Long {
        val drivingTime = event.drivingTime!!.toDouble()
        val distance = event.distance!!.toDouble()
        val speed = if (drivingTime > 0 && distance > 0)
            distance / 1000 / (drivingTime / 60)
        else
            mDefaultSpeed

        return (distanceToEvent / 1000.0 / speed).toLong() * 60 * 1000
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
    private fun determineDistanceToEvent(event: Event, location: Location?): Int {
        //sanity check in case the event does not have a latlng, in that case will try to
        //get it here if not don't track
        val eventLatlng = (if (event.locationLatlng != null)
            event.locationLatlng
        else
            LocationUtils.latlngFromAddress(mContext, event.location)) ?: return -1
        val eventLocation = Location("")
        eventLocation.latitude = eventLatlng.latitude
        eventLocation.longitude = eventLatlng.longitude
        return location!!.distanceTo(eventLocation).toInt()
    }

    private fun showEventNotification(event: Event, type: Int, pastEndTime: Boolean?) {
        if (alreadyNotified(event.id.toString())) return
        val intent = Intent(mContext, EventDetailActivity::class.java)
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson(event))
        val pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val builder = NotificationCompat.Builder(mContext, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_geofence_car)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (type == EVENT_TIME) {
            builder.setContentTitle(mContext.getString(R.string.driving_notification_title_missed))
            if (pastEndTime!!) {
                builder.setContentText(mContext.getString(R.string.driving_notification_content_missed_end))
                        .setStyle(NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.driving_notification_content_missed_end)))
            } else {
                builder.setContentText(mContext.getString(R.string.driving_notification_content_missed_start))
                        .setStyle(NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.driving_notification_content_missed_start)))
            }

        } else {
            builder.setContentTitle(mContext.getString(R.string.driving_nofication_time_to_go_title))
                    .setContentText(mContext.getString(R.string.driving_nofication_time_to_go_content))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(mContext.getString(R.string.driving_nofication_time_to_go_content)))
        }
        NotificationManagerCompat.from(mContext).notify(0, builder.build())
    }

    private fun alreadyNotified(id: String): Boolean {
        val set = mSharedPreferences!!.getStringSet(Constants.DISABLED_DRIVING_EVENTS, HashSet())
        if (set!!.contains(id)) return true
        set.add(id)
        mSharedPreferences.edit().putStringSet(Constants.DISABLED_DRIVING_EVENTS, set).apply()
        return false
    }
}
