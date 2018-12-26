package com.aaronbrecher.neverlate.Utils

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.text.format.DateUtils
import android.util.SparseArray

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.models.EventLocationDetails
import com.aaronbrecher.neverlate.models.retrofitmodels.EventDistanceDuration
import com.aaronbrecher.neverlate.network.AppApiService
import com.aaronbrecher.neverlate.network.*
import com.google.android.gms.maps.model.LatLng

import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

import retrofit2.Call
import retrofit2.Response

/**
 * Class that contains functions to get distance information,
 * will use the Here api https://developer.here.com/
 */
class DirectionsUtils(mSharedPreferences: SharedPreferences, private val mLocation: Location?) {
    private val mspeed: Double = java.lang.Double.valueOf(mSharedPreferences.getString(Constants.SPEED_PREFS_KEY, "0.666667")!!)

    /**
     * function to add distance information (Distance,Duration) to events
     * TODO for subscriptions - will execute this code always, if server response is that the user is not subscribed than will use as the crow flies data instead
     *
     * @param events   list of events from the calendar
     */
    fun addDistanceInfoToEventList(events: List<Event>): Boolean {
        if (mLocation == null) return false
        val filtered = removeEventsWithoutLocation(events)
        val transitTypes = splitEventListByTrasportType(filtered)
        return executeDrivingQuery(transitTypes.get(Constants.TRANSPORT_DRIVING)) && executeTransitQuery(transitTypes.get(Constants.TRANSPORT_PUBLIC))
    }

    //Filter out all events that do not have a valid location
    private fun removeEventsWithoutLocation(eventList: List<Event>): List<Event> {
        return eventList.filter { it.locationLatlng != null }
    }

    /**
     * This query will be to check for driving
     *
     * @return true if the data was added (even partially) false if not
     */
    private fun executeDrivingQuery(events: List<Event>): Boolean {
        return executeHereMatrixQuery(events, false)
    }

    /**
     * This query will be to check for public transportation data
     *
     * @return true if the data was added (even partially) false if not
     */
    private fun executeTransitQuery(events: List<Event>): Boolean {
        return executeHereMatrixQuery(events, true)
    }


    private fun executeHereMatrixQuery(events: List<Event>, forPublicTransit: Boolean): Boolean {
        if (events.isEmpty()) return true
        val origin = mLocation!!.latitude.toString() + "," + mLocation.longitude
        val destinations = convertEventListForQuery(events, forPublicTransit)
        val service = createRetrofitService()

        val request = if (forPublicTransit)
            service.queryHerePublicTransit(origin, destinations)
        else
            service.queryHereMatrix(origin, destinations)
        try {
            val response = request.execute()
            if (response.code() == 403) {
                addCrowFliesDistanceInfo(events)
                return true
            }
            val durationList = response.body()
            if (durationList == null || durationList.isEmpty()) return false
            for (i in durationList.indices) {
                val event = events[i]
                val (distance, duration) = durationList[i]
                event.distance = distance.toLong()
                event.drivingTime = duration.toLong()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    private fun addCrowFliesDistanceInfo(events: List<Event>) {
        for (event in events) {
            val eventLocation = Location("never-late")
            eventLocation.latitude = event.locationLatlng!!.latitude
            eventLocation.longitude = event.locationLatlng!!.longitude
            val distance = mLocation!!.distanceTo(eventLocation).toLong()
            if (distance <= 0) continue
            val distanceInKilometers = distance / 1000
            val time = (distanceInKilometers / mspeed).toLong() * 60
            event.drivingTime = time
            event.distance = distance
        }
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
    private fun splitEventListByTrasportType(eventList: List<Event>): SparseArray<List<Event>> {
        val drivingEvents = ArrayList<Event>()
        val publicEvents = ArrayList<Event>()
        val splitMap = SparseArray<List<Event>>()
        for (event in eventList) {
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
