package com.aaronbrecher.neverlate.Utils

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.SparseArray
import com.aaronbrecher.neverlate.AppExecutors

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.billing.BillingManager
import com.aaronbrecher.neverlate.billing.BillingUpdatesListener
import com.aaronbrecher.neverlate.interfaces.DistanceInfoAddedListener
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.models.EventLocationDetails
import com.aaronbrecher.neverlate.models.HereApiBody
import com.aaronbrecher.neverlate.models.PurchaseData
import com.aaronbrecher.neverlate.network.AppApiService
import com.aaronbrecher.neverlate.network.*
import com.google.firebase.analytics.FirebaseAnalytics

import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

/**
 * Class that contains functions to get distance information,
 * will use the Here api https://developer.here.com/
 */
class DirectionsUtils(mSharedPreferences: SharedPreferences,
                      private val mLocation: Location?,
                      private val distanceInfoAddedListener: DistanceInfoAddedListener,
                      private val context: Context) : BillingUpdatesListener {

    private val mspeed: Double = mSharedPreferences.getString(Constants.SPEED_PREFS_KEY, "0.666667")!!.toDouble()
    private val retrofitService: AppApiService = createRetrofitService()
    private lateinit var billingManager: BillingManager
    private lateinit var filteredEvents: List<Event>
    private val purchaseList: MutableList<PurchaseData> = ArrayList()
    private val appExecutors = AppExecutors()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    /**
     * function to add distance information (Distance,Duration) to events
     *
     * @param events   list of events from the calendar
     */
    fun addDistanceInfoToEventList(events: List<Event>) {
        if (mLocation == null) return
        filteredEvents = removeEventsWithoutLocation(events)
//      TODO when want to start billing uncomment and change subscribe button to enabled in xml
//      billingManager = BillingManager(context, this)

//      TODO when want to start billing remove this
        appExecutors.networkIO().execute {
            val responseType = addDistanceFromHereApi()
            when (responseType) {
                QueryResponseType.SUCCESS -> distanceInfoAddedListener.distanceUpdated()
                QueryResponseType.FAILED,
                QueryResponseType.UNVERIFIED -> addCrowFliesDistanceInfo(filteredEvents)

            }
        }

    }

    override fun onBillingClientSetupFinished() {
        billingManager.getSubList(false)
    }

    override fun onPurchasesUpdated(purchases: List<PurchaseData>, wasAsync: Boolean) {
        appExecutors.networkIO().execute {
            purchaseList.addAll(purchases)
            val responseType = addDistanceFromHereApi()
            when (responseType) {
                QueryResponseType.SUCCESS -> distanceInfoAddedListener.distanceUpdated()
                QueryResponseType.FAILED -> addCrowFliesDistanceInfo(filteredEvents)
                QueryResponseType.UNVERIFIED -> {
                    if (wasAsync) addCrowFliesDistanceInfo(filteredEvents)
                    else mainThreadHandler.post { billingManager.getSubList(true) }
                }
            }
        }
    }

    override fun onBillingSetupFailed() {
        addCrowFliesDistanceInfo(filteredEvents)
    }

    private fun addDistanceFromHereApi(): QueryResponseType {
        val transitTypes = splitEventListByTransportType()
        val drivingQueryResponseType = executeDrivingQuery(transitTypes.get(Constants.TRANSPORT_DRIVING))
        val transitQueryResponseType = executeTransitQuery(transitTypes.get(Constants.TRANSPORT_PUBLIC))
        return if (drivingQueryResponseType == QueryResponseType.UNVERIFIED
                || transitQueryResponseType == QueryResponseType.UNVERIFIED) {
            QueryResponseType.UNVERIFIED
        } else if (drivingQueryResponseType == QueryResponseType.SUCCESS || transitQueryResponseType == QueryResponseType.SUCCESS) {
            QueryResponseType.SUCCESS
        } else {
            QueryResponseType.FAILED
        }
    }

    //Filter out all events that do not have a valid location
    private fun removeEventsWithoutLocation(eventList: List<Event>): List<Event> {
        return eventList.filter { it.locationLatlng != null }
    }

    /**
     * This query will be to check for driving
     * @return true if the data was added (even partially) false if not
     */
    private fun executeDrivingQuery(events: List<Event>): QueryResponseType {
        return executeHereMatrixQuery(events, false)
    }

    /**
     * This query will be to check for public transportation data
     * @return true if the data was added (even partially) false if not
     */
    private fun executeTransitQuery(events: List<Event>): QueryResponseType {
        return executeHereMatrixQuery(events, true)
    }


    private fun executeHereMatrixQuery(events: List<Event>, forPublicTransit: Boolean): QueryResponseType {
        if (events.isEmpty()) return QueryResponseType.SUCCESS
        val origin = mLocation!!.latitude.toString() + "," + mLocation.longitude
        val destinations = convertEventListForQuery(events, forPublicTransit)
        val request = if (forPublicTransit)
            retrofitService.queryHerePublicTransit(origin, HereApiBody(destinations, purchaseList))
        else
            retrofitService.queryHereMatrix(origin, HereApiBody(destinations, purchaseList))
        try {
            val response = request.execute()
            if (response.code() == 403) {
//                addCrowFliesDistanceInfo(events)
                return QueryResponseType.UNVERIFIED
            }
            val durationList = response.body()
            if (durationList == null || durationList.isEmpty()) return QueryResponseType.FAILED
            for (i in durationList.indices) {
                val event = events[i]
                val (distance, duration) = durationList[i]
                event.distance = distance.toLong()
                event.drivingTime = duration.toLong()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return QueryResponseType.FAILED
        }

        return QueryResponseType.SUCCESS
    }

    private fun addCrowFliesDistanceInfo(events: List<Event>) {
        events.forEach {
            val eventLocation = Location("never-late")
            eventLocation.latitude = it.locationLatlng!!.latitude
            eventLocation.longitude = it.locationLatlng!!.longitude
            val distance = mLocation!!.distanceTo(eventLocation).toLong()
            if (distance > 0) {
                val distanceInKilometers = distance / 1000
                val time = (distanceInKilometers / mspeed).toLong() * 60
                it.drivingTime = time
                it.distance = distance
            }
        }
        appExecutors.diskIO().execute { distanceInfoAddedListener.distanceUpdated() }
    }

    /**
     * convert the event list to a more concise representation to send to server, will include the latitude,
     * longitude, as well as the arrival time
     *
     * @param events             list of all the events
     * @param forPublicTransport if for a public transit request need to add the arrival time in iso-format
     * @return a list of the minimized event objects
     */
    private fun convertEventListForQuery(events: List<Event>, forPublicTransport: Boolean): List<EventLocationDetails> {
        val destinations = ArrayList<EventLocationDetails>()
        for (event in events) {
            val eventTime = GeofenceUtils.determineRelevantTime(event.startTime, event.endTime)
            val locationDetails = EventLocationDetails(event.locationLatlng!!.latitude.toString(),
                    event.locationLatlng!!.longitude.toString())
            if (forPublicTransport) {
                //TODO create iso-time for event location
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
                val formatted = dateFormat.format(Date(eventTime))
                locationDetails.arrivalTime = formatted
            }
            destinations.add(locationDetails)
        }
        return destinations
    }

    /**
     * Function to split the events into different driving catagories
     *
     * @return a map containing lists corresponding to all driving types
     */
    private fun splitEventListByTransportType(): SparseArray<List<Event>> {
        val drivingEvents = ArrayList<Event>()
        val publicEvents = ArrayList<Event>()
        val splitMap = SparseArray<List<Event>>()
        for (event in filteredEvents) {
            when (event.transportMode) {
                Constants.TRANSPORT_PUBLIC -> publicEvents.add(event)
                Constants.TRANSPORT_DRIVING -> drivingEvents.add(event)
                else -> drivingEvents.add(event)
            }
        }
        splitMap.put(Constants.TRANSPORT_DRIVING, drivingEvents)
        splitMap.put(Constants.TRANSPORT_PUBLIC, publicEvents)
        return splitMap
    }

    companion object {


        //TODO move these functions to a different class

        /**
         * Returns a readable string of distance to the event either in
         * Miles or KM
         */
        fun getHumanReadableDistance(context: Context, distance: Long, sharedPreferences: SharedPreferences): String {
            //TODO add a shared prefs to miles or km and fix this accordingly
            var useMetric = true

            val unitType = sharedPreferences.getString(context.getString(R.string.pref_units_key), "")
            if (context.getString(R.string.pref_units_imperial) == unitType) {
                useMetric = false
            }
            val km = distance.toFloat() / 1000
            val df = DecimalFormat("#.#")
            return if (useMetric) {
                df.format(km.toDouble()) + " " + context.getString(R.string.km_signature)
            } else {
                val miles = LocationUtils.kmToMiles(km)
                df.format(miles) + " " + context.getString(R.string.miles_signature)
            }
        }

        /**
         * Returns a human readable representation of the time to leave to the
         * event
         *
         * @param timeTo    time until the event in seconds
         * @param eventTime time of the event in millis
         * @return Text of how much time to leave
         */
        fun getTimeToLeaveHumanReadable(timeTo: Long, eventTime: Long): String {
            val timeToMillis = timeTo * 1000
            val leaveTime = eventTime - timeToMillis
            return DateUtils.getRelativeTimeSpanString(leaveTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

        }


        fun readableTravelTime(travelTime: Long): String {
            val totalMinutes = (travelTime / 60).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return hours.toString() + ":" + minutes
        }
    }
}

enum class QueryResponseType {
    SUCCESS, FAILED, UNVERIFIED
}
