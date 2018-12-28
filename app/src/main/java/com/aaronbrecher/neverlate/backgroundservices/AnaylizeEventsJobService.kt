package com.aaronbrecher.neverlate.backgroundservices

import android.location.Location

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.billing.BillingManager
import com.aaronbrecher.neverlate.billing.BillingUpdatesListener
import com.aaronbrecher.neverlate.database.Converters
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.models.EventCompatibility
import com.aaronbrecher.neverlate.models.EventLocationDetails
import com.aaronbrecher.neverlate.models.retrofitmodels.EventDistanceDuration
import com.aaronbrecher.neverlate.network.AppApiService
import com.aaronbrecher.neverlate.network.*
import com.aaronbrecher.neverlate.ui.activities.MainActivity
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.android.gms.maps.model.LatLng

import java.io.IOException
import java.util.ArrayList

import javax.inject.Inject

import retrofit2.Call

class AnaylizeEventsJobService : JobService(), BillingUpdatesListener {

    @Inject
    lateinit var mEventsRepository: EventsRepository
    @Inject
    lateinit var mAppExecutors: AppExecutors
    @Inject
    lateinit var mCompatabilityRepository: EventCompatibilityRepository

    private lateinit var mApiService: AppApiService
    private lateinit var mJobParameters: JobParameters
    private lateinit var mBillingManager: BillingManager
    private var mEventList: List<Event> = ArrayList()
    private var mEventCompatibilities: MutableList<EventCompatibility>? = null

    override fun onCreate() {
        super.onCreate()
        NeverLateApp.app.appComponent
                .inject(this)
        mApiService = createRetrofitService()
    }

    override fun onStartJob(job: JobParameters): Boolean {
        mJobParameters = job
        mBillingManager = BillingManager(this, this)
        return true
    }

    override fun onBillingClientSetupFinished() {
        mBillingManager.verifySub()
    }

    override fun onBillingSetupFailed() {
        MainActivity.setFinishedLoading(true)
        jobFinished(mJobParameters, false)
    }

    override fun onSubscriptionVerified(isVerified: Boolean) {
        if(isVerified){
            mAppExecutors.diskIO().execute { this.doWork() }
        } else{
            MainActivity.setFinishedLoading(true)
            jobFinished(mJobParameters, false)
        }
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return false
    }

    private fun doWork() {
        mEventCompatibilities = ArrayList()
        mEventList = mEventsRepository.queryAllCurrentTrackedEventsSync()
        if (mEventList.size < 2) {
            mCompatabilityRepository.deleteAll()
            mAppExecutors.mainThread().execute { MainActivity.setFinishedLoading(true) }
            return
        }
        for (i in 0 until mEventList.size - 1) {
            val compatibility = getCompatibility(mEventList[i], mEventList[i + 1])
            if (compatibility != null) {
                mEventCompatibilities!!.add(compatibility)
            }
        }
        //todo figure out a better way of doing this
        mCompatabilityRepository.deleteAll()
        //TODO try/catch is to prevent crashes due to null pointer in dao, need to find the actual cause of this
        try {
            mCompatabilityRepository.insertAll(mEventCompatibilities!!)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        mAppExecutors.mainThread().execute { MainActivity.setFinishedLoading(true) }
        jobFinished(mJobParameters, false)
    }

    private fun getCompatibility(event1: Event, event2: Event): EventCompatibility? {
        val eventCompatibility = EventCompatibility()
        eventCompatibility.startEvent = event1.id
        eventCompatibility.endEvent = event2.id

        val originLatLng = event1.locationLatlng
        val destinationLatLng = event2.locationLatlng
        if (originLatLng == null || destinationLatLng == null) return null
        val origin = originLatLng.latitude.toString() + "," + originLatLng.longitude
        var duration = getDrivingDuration(origin, event2)
        if (duration < 1) {
            eventCompatibility.withinDrivingDistance = EventCompatibility.Compatible.UNKNOWN
        } else {
            duration *= 1000
            determineComparabilityAndTiming(event1, event2, eventCompatibility, duration.toDouble())
        }
        return eventCompatibility
    }

    private fun getDrivingDuration(origin: String, event: Event): Int {
        val details = EventLocationDetails(event.locationLatlng!!.latitude.toString(),
                event.locationLatlng!!.longitude.toString())
        val call = mApiService.queryDirections(origin, details)
        try {
            val duration = call.execute().body()
            if (duration != null) return duration.duration
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return -1
    }

    private fun determineComparabilityAndTiming(event1: Event, event2: Event, eventCompatibility: EventCompatibility, duration: Double) {
        val firstEventStart = Converters.unixFromDateTime(event1.startTime)!!
        val secondEventStart = Converters.unixFromDateTime(event2.startTime)!!
        val arrivalTimeToSecondEvent = firstEventStart + duration.toLong()
        if (arrivalTimeToSecondEvent > secondEventStart) {
            eventCompatibility.withinDrivingDistance = EventCompatibility.Compatible.FALSE
            eventCompatibility.canReturnHome = false
            eventCompatibility.canReturnToWork = false
            eventCompatibility.maxTimeAtStartEvent = Constants.ROOM_INVALID_LONG_VALUE
        } else {
            eventCompatibility.withinDrivingDistance = EventCompatibility.Compatible.TRUE
            val maximumTimeAtEvent = secondEventStart - arrivalTimeToSecondEvent
            //TODO Setting this to false to avoid bugs in future fix this
            eventCompatibility.canReturnHome = false
            eventCompatibility.canReturnToWork = false
            eventCompatibility.maxTimeAtStartEvent = maximumTimeAtEvent
        }

    }

    /**
     * Gets the "as the crow flies" distance between two events
     *
     * @return the distance in KM
     */
    private fun getCrowFlyDistance(origin: LatLng, destination: LatLng): Float {
        val locationorigin = Location("never-late")
        val locationDest = Location("never-late")
        locationorigin.latitude = origin.latitude
        locationorigin.longitude = origin.longitude
        locationDest.latitude = destination.latitude
        locationDest.longitude = origin.longitude
        return locationorigin.distanceTo(locationDest) / 1000

    }
}
