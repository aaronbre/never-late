package com.aaronbrecher.neverlate

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import androidx.core.app.ActivityCompat
import android.text.TextUtils
import android.widget.Toast

import com.aaronbrecher.neverlate.utils.DirectionsUtils
import com.aaronbrecher.neverlate.utils.GeofenceUtils
import com.aaronbrecher.neverlate.utils.LocationUtils
import com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers.StartJobIntentServiceBroadcastReceiver
import com.aaronbrecher.neverlate.database.Converters
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.interfaces.DistanceInfoAddedListener
import com.aaronbrecher.neverlate.models.Event
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.FenceClient
import com.google.android.gms.awareness.fence.AwarenessFence
import com.google.android.gms.awareness.fence.FenceUpdateRequest
import com.google.android.gms.awareness.fence.LocationFence
import com.google.android.gms.awareness.fence.TimeFence
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnSuccessListener

import java.util.ArrayList
import java.util.ConcurrentModificationException

import javax.inject.Inject

@WorkerThread
class AwarenessFencesCreator private constructor(var eventList: List<Event>) : DistanceInfoAddedListener {
    @Inject
    lateinit var mApp: NeverLateApp
    @Inject
    lateinit var mLocationProviderClient: FusedLocationProviderClient
    @Inject
    lateinit var mSharedPreferences: SharedPreferences
    @Inject
    lateinit var mEventsRepository: EventsRepository
    @Inject
    lateinit var mAppExecutors: AppExecutors

    private val mFenceClient: FenceClient
    private var mLocation: Location? = null
    private var mPendingIntent: PendingIntent? = null
    private val mAlertTime: Long

    private val alertTime: Long
        get() {
            val alertTime = mSharedPreferences.getString(Constants.ALERTS_PREFS_KEY, "")
            return when (alertTime) {
                Constants.ALERT_TIME_SHORT -> Constants.TIME_FIVE_MINUTES
                Constants.ALERT_TIME_MEDIUM -> Constants.TIME_TEN_MINUTES
                Constants.ALERT_TIME_LONG -> Constants.TIME_FIFTEEN_MINUTES
                else -> Constants.TIME_TEN_MINUTES
            }
        }

    private val pendingIntent: PendingIntent?
        get() {
            if (mPendingIntent != null) return mPendingIntent
            val intent = Intent(mApp, StartJobIntentServiceBroadcastReceiver::class.java)
            intent.action = Constants.ACTION_START_AWARENESS_FENCE_SERVICE
            mPendingIntent = PendingIntent.getBroadcast(mApp,
                    Constants.AWARENESS_TRANSITION_PENDING_INTENT_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            return mPendingIntent
        }

    init {
        NeverLateApp.app.appComponent.inject(this)
        mFenceClient = Awareness.getFenceClient(mApp)
        mAlertTime = alertTime
    }

    /**
     * Ideally the location will always be set by the Activity Recognition and AlarmService,
     * In edge case where it was not available then will try to update now
     */
    @SuppressLint("MissingPermission")
    @WorkerThread
    fun buildAndSaveFences(transportChange: Boolean = false) {
        mLocationProviderClient.lastLocation.addOnSuccessListener(mAppExecutors.diskIO(), OnSuccessListener { location ->
            location?.let {
                mLocation = it
                //TODO this is probably not needed, as every call to this already added distance info
                // If the location is older then a day we can assume that distance OR if the save was
                // for a transport change need to update
                if (it.time < System.currentTimeMillis() - Constants.ONE_DAY || transportChange) {
                    val directionsUtils = DirectionsUtils(mSharedPreferences, it, this, mApp)
                    Handler(Looper.getMainLooper()).run { directionsUtils.addDistanceInfoToEventList(eventList)}
                }else{
                    mEventsRepository.insertAll(eventList)
                    updateFences()
                }
            }
        })
    }

    override fun distanceUpdated() {
        mAppExecutors.diskIO().execute {
            mEventsRepository.insertAll(eventList)
            updateFences()
        }
    }

    private fun createFences(): List<AwarenessFenceWithName> {
        val fenceList = ArrayList<AwarenessFenceWithName>()
        for (event in eventList) {
            //added try/catch if creating a fence fails
            try {
                //all events will start with the value set to ROOM_INVALID... as a sentinal
                if (event.drivingTime == Constants.ROOM_INVALID_LONG_VALUE) continue
                var fenceName = Constants.AWARENESS_FENCE_MAIN_PREFIX + event.id
                val relevantTime = GeofenceUtils.determineRelevantTime(event.startTime, event.endTime)
                var triggerTime = relevantTime - event.drivingTime!! * 1000
                val fence = AwarenessFenceWithName(createAwarenessFenceForEvent(triggerTime), fenceName)
                val arrivalFenceName = Constants.AWARENESS_FENCE_ARRIVAL_PREFIX + event.id
                val arrivalFence = AwarenessFenceWithName(createArrivalFenceForEvent(event), arrivalFenceName)
                fenceList.add(arrivalFence)
                fenceList.add(fence)

                if (relevantTime == Converters.unixFromDateTime(event.startTime)) {
                    fenceName = Constants.AWARENESS_FENCE_END_PREFIX + event.id
                    triggerTime = Converters.unixFromDateTime(event.endTime)!! - event.drivingTime!! * 1000
                    val endFence = AwarenessFenceWithName(createAwarenessFenceForEvent(triggerTime), fenceName)
                    fenceList.add(endFence)
                }
            } catch (e: IllegalArgumentException) {
                //todo fix conccurent error
                e.printStackTrace()
            } catch (e: ConcurrentModificationException) {
                e.printStackTrace()
            }

        }
        return fenceList
    }

    /**
     * creates an awarenessFence it will have two triggers : Time and location, the triggerTime will be the time
     * of the event - the time it takes to drive there. The location will be the users current location which is
     * saved in the app class
     *
     * @param triggerTime the triggerTime will either be the event start or end
     * @return
     */
    private fun createAwarenessFenceForEvent(triggerTime: Long): AwarenessFence? {
        if (ActivityCompat.checkSelfPermission(mApp, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        if (triggerTime < System.currentTimeMillis()) return null
        val locationFence = LocationFence.`in`(mLocation!!.latitude,
                mLocation!!.longitude,
                Constants.LOCATION_FENCE_RADIUS.toDouble(),
                Constants.LOCATION_FENCE_DWELL_TIME)
        //TODO the end time is such for testing purposes in reality possibly will end the time before this
        val timeFence = TimeFence.inInterval(triggerTime - mAlertTime, triggerTime + Constants.TIME_FIFTEEN_MINUTES)
        return AwarenessFence.and(locationFence, timeFence)
    }

    /**
     * This will create an additional fence for each event for arrival at event,
     * this will be used to remove the event from tracking when the user makes it
     *
     * @param event the event to create fence for
     * @return
     */
    private fun createArrivalFenceForEvent(event: Event): AwarenessFence? {
        val latLng = (if (event.locationLatlng != null)
            event.locationLatlng
        else
            LocationUtils.latlngFromAddress(mApp, event.location)) ?: return null

        val startTime = Converters.unixFromDateTime(event.startTime)!!
        val endTime = Converters.unixFromDateTime(event.endTime)!!
        if (startTime < System.currentTimeMillis() || endTime < System.currentTimeMillis()) return null
        @SuppressLint("MissingPermission") val locationFence = LocationFence.`in`(latLng.latitude,
                latLng.longitude,
                Constants.ARRIVAL_FENCE_RADIUS.toDouble(),
                Constants.ARRIVAL_FENCE_DWELL_TIME)
        val timeFence = TimeFence.inInterval(startTime - Constants.TIME_TEN_MINUTES,
                endTime)
        return AwarenessFence.and(locationFence, timeFence)

    }

    private fun getUpdateRequest(fences: List<AwarenessFenceWithName>): FenceUpdateRequest? {
        val builder = FenceUpdateRequest.Builder()
        for (fence in fences) {
            if (fence.fence == null || fence.name == null || TextUtils.isEmpty(fence.name))
                continue
            builder.addFence(fence.name, fence.fence, pendingIntent)
        }
        return builder.build()
    }


    private fun updateFences() {
        val fencelist = createFences()
        if (fencelist.isEmpty()) return
        val request = getUpdateRequest(fencelist) ?: return
        mFenceClient.updateFences(request).addOnSuccessListener {
            if (!mApp.isInBackground) {
                if (fencelist.size < eventList.size)
                    Toast.makeText(mApp, R.string.geofence_added_partial_success, Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(mApp, R.string.geofence_added_success, Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(mApp, R.string.geofence_added_failed, Toast.LENGTH_SHORT).show()
        }
    }


    @WorkerThread
    fun removeFences(vararg events: Event) {
        val builder = FenceUpdateRequest.Builder()
        events.forEach {
            builder.removeFence(Constants.AWARENESS_FENCE_MAIN_PREFIX + it.id)
            builder.removeFence(Constants.AWARENESS_FENCE_ARRIVAL_PREFIX + it.id)
            builder.removeFence(Constants.AWARENESS_FENCE_END_PREFIX + it.id)
        }
        mFenceClient.updateFences(builder.build())
    }

    class Builder(private val mEventList: List<Event>?) {

        fun build(): AwarenessFencesCreator {
            return if (mEventList == null) AwarenessFencesCreator(ArrayList()) else AwarenessFencesCreator(mEventList)
        }
    }
}

/**
 * This subclass is used to make it easier to add a geofence with a name
 * built from the Event. All names will have a prefix + the event ID
 */
private class AwarenessFenceWithName(var fence: AwarenessFence?, var name: String?)

