package com.aaronbrecher.neverlate.backgroundservices


import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.utils.BackgroundUtils
import com.aaronbrecher.neverlate.utils.CalendarUtils
import com.aaronbrecher.neverlate.utils.DirectionsUtils
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.AwarenessFencesCreator
import com.aaronbrecher.neverlate.interfaces.DistanceInfoAddedListener
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.ui.activities.MainActivity
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.Tasks
import java.lang.Exception

import javax.inject.Inject

/**
 * Service which will check peroidically if any new events where added
 * as well as if any event has been changed
 */
class CheckForCalendarChangedService : JobService(), DistanceInfoAddedListener {

    @Inject
    internal lateinit var mEventsRepository: EventsRepository
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences
    @Inject
    internal lateinit var mAppExecutors: AppExecutors

    @Inject
    internal lateinit var mLocationProviderClient: FusedLocationProviderClient

    private lateinit var mLocationCallback: LocationCallback

    private lateinit var mJobParameters: JobParameters
    private var mGeofenceList: MutableList<Event> = ArrayList()
    private var mNoGeofenceList: MutableList<Event> = ArrayList()

    override fun onCreate() {
        super.onCreate()
        NeverLateApp.app.appComponent
                .inject(this)
        Log.i(TAG, "onCreate: Check for calendar job")
    }

    override fun onStartJob(job: JobParameters): Boolean {
        mJobParameters = job
        mAppExecutors.diskIO().execute { this.doWork() }
        return true
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return false
    }

    @SuppressLint("MissingPermission")
    private fun doWork() {
        Log.i(TAG, "doWork: checking for calendar changes")
        val oldList = mEventsRepository.queryEventsNoLocationSync()
        val newList = CalendarUtils.getCalendarEventsForToday(this)
        //first need to check if the 2 lists are the same or if different what type of update
        //needed
        val listsToAdd = CalendarUtils.compareCalendars(oldList.toMutableList(), newList.toMutableList())
        mGeofenceList = listsToAdd[Constants.LIST_NEEDS_FENCE_UPDATE]?.toMutableList() ?: ArrayList()
        mNoGeofenceList = listsToAdd[Constants.LIST_NO_FENCE_UPDATE]?.toMutableList() ?: ArrayList()
        try {
            val location = Tasks.await(mLocationProviderClient.lastLocation)
            if (location == null || location.time < System.currentTimeMillis() - Constants.FIVE_HOUR) {
                //if the location is invalid need to update the fences for all lists
                mGeofenceList = newList.toMutableList()
                mNoGeofenceList = ArrayList()
                getNewLocation(mGeofenceList)
            } else if (mGeofenceList.isNotEmpty()) {
                setOrRemoveFences(mGeofenceList, location)
            } else if (mNoGeofenceList.size > 0) {
                mEventsRepository.deleteAllEvents()
                mEventsRepository.insertAll(mNoGeofenceList)
                jobFinished(mJobParameters, false)
            } else {
                mEventsRepository.deleteAllEvents()
                mAppExecutors.mainThread().execute { MainActivity.setFinishedLoading(true) }
                jobFinished(mJobParameters, false)
            }
        } catch (e: Exception) {
            //notify user that fences where not updated
            e.printStackTrace()
            mEventsRepository.deleteAllEvents()
            mAppExecutors.mainThread().execute { MainActivity.setFinishedLoading(true) }
            jobFinished(mJobParameters, false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNewLocation(newList: List<Event>) {
        mLocationProviderClient.requestLocationUpdates(LocationRequest().setInterval(10)
                .setNumUpdates(1)
                .setExpirationDuration(15000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                mLocationCallback, Looper.getMainLooper())
                .addOnCompleteListener {
                    MainActivity.setFinishedLoading(true)
                    jobFinished(mJobParameters, false)
                }

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                mAppExecutors.diskIO().execute {
                    val location = locationResult!!.lastLocation
                    //if we have a new location then create fence for a all lists
                    Log.i(TAG, "onLocationResult: recieved")
                    setOrRemoveFences(newList, location)
                }
            }
        }
    }

    /**
     * add the distance data to the list and if all good update fences,
     * if unable to get data then remove the fences as no longer relevant,
     * NOTE - need to set distance data here even though the creator does it in the
     * event that there is no location. That is only a fix if there is no location however
     * here it is needed because the event information changed
     *
     * @param eventsToAddWithGeofences
     * @param location
     */
    private fun setOrRemoveFences(eventsToAddWithGeofences: List<Event>, location: Location) {
        val directionsUtils = DirectionsUtils(mSharedPreferences, location, this, this)
        //This NEEDS to be run on UI thread for BillingClient
        Handler(Looper.getMainLooper()).post { directionsUtils.addDistanceInfoToEventList(eventsToAddWithGeofences) }
    }

    override fun distanceUpdated() {
        val fencesCreator = AwarenessFencesCreator.Builder(null).build()
        fencesCreator.eventList = mGeofenceList
        fencesCreator.buildAndSaveFences()
        //delete old events
        mEventsRepository.deleteAllEvents()
        //combine lists to make one upload
        mGeofenceList.addAll(mNoGeofenceList)
        mEventsRepository.insertAll(mGeofenceList)
        //if there was a change refresh the analytics
        //TODO when the bug is found fix this
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        dispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(dispatcher))
        jobFinished(mJobParameters, false)
    }

    companion object {
        private val TAG = CheckForCalendarChangedService::class.java.simpleName
    }
}
