package com.aaronbrecher.neverlate.backgroundservices.jobintentservices

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.core.app.JobIntentService

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.Utils.DirectionsUtils
import com.aaronbrecher.neverlate.Utils.SystemUtils
import com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers.DrivingLocationUpdatesBroadcastReceiver
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.AwarenessFencesCreator
import com.aaronbrecher.neverlate.interfaces.DistanceInfoAddedListener
import com.aaronbrecher.neverlate.models.Event
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.OnSuccessListener

import javax.inject.Inject

class ActivityTransitionService : JobIntentService(), DistanceInfoAddedListener {
    @Inject
    internal lateinit var mLocationProviderClient: FusedLocationProviderClient
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences
    @Inject
    internal lateinit var mEventsRepository: EventsRepository
    @Inject
    internal lateinit var mAppExecutors: AppExecutors

    private lateinit var mEventList: List<Event>

    private val pendingIntent: PendingIntent
        get() {
            val intent = Intent(this, DrivingLocationUpdatesBroadcastReceiver::class.java)
            intent.action = Constants.ACTION_PROCESS_LOCATION_UPDATE
            return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    override fun onCreate() {
        super.onCreate()
        NeverLateApp.app.appComponent.inject(this)
    }

    override fun onHandleWork(@NonNull intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            //for our purposes only the last event should be considered, if user already parked previous data
            //is not relevant and vice versa
            val events = result?.transitionEvents
            val event = events?.last()
            //only execute code for the in-vehicle activity
            if (event?.activityType != DetectedActivity.IN_VEHICLE) return

            if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                setUpFences()
                stopLocationUpdates()
                clearDisabledEvents()
            } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                requestLocationUpdates()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (!SystemUtils.hasLocationPermissions(this)) return
        mLocationProviderClient.requestLocationUpdates(createLocationRequest(), pendingIntent)
    }

    private fun createLocationRequest(): LocationRequest {
        val request = LocationRequest()
        request.setInterval(Constants.TIME_FIVE_MINUTES)
                .setFastestInterval(Constants.TIME_FIVE_MINUTES)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).smallestDisplacement = (Constants.LOCATION_FENCE_RADIUS / 2).toFloat()
        return request
    }


    private fun stopLocationUpdates() {
        mLocationProviderClient.removeLocationUpdates(pendingIntent)
    }

    private fun clearDisabledEvents() {
        mSharedPreferences.edit().putStringSet(Constants.DISABLED_DRIVING_EVENTS, null).apply()
    }


    /**
     * If user has stopped driving assume he will remain in this location and update the
     * eventList with the distance info. Reset the AwarenessFences using the current
     * location information and current Location
     */
    @SuppressLint("MissingPermission")
    private fun setUpFences() {

        if (!SystemUtils.hasLocationPermissions(this)) {
            return
        }

        mLocationProviderClient.lastLocation.addOnSuccessListener(mAppExecutors.diskIO(), OnSuccessListener { location ->
            mEventList = mEventsRepository.queryAllCurrentEventsSync()
            val directionsUtils = DirectionsUtils(mSharedPreferences, location, this, this)
            Handler(Looper.getMainLooper()).post { directionsUtils.addDistanceInfoToEventList(mEventList) }
        })
    }

    override fun distanceUpdated() {
            mAppExecutors.diskIO().run {
                val creator = AwarenessFencesCreator.Builder(mEventList).build()
                creator.eventList = mEventList
                creator.buildAndSaveFences()
            }
    }

    companion object {
        private var JOB_ID = 1001

        fun enqueueWork(context: Context, work: Intent) {
            JobIntentService.enqueueWork(context, ActivityTransitionService::class.java, JOB_ID, work)
        }
    }
}
