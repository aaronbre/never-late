package com.aaronbrecher.neverlate.Utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import android.util.Pair

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.database.Converters
import com.aaronbrecher.neverlate.AwarenessFencesCreator
import com.aaronbrecher.neverlate.models.Event
import com.google.android.gms.maps.model.LatLng

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap


object CalendarUtils {

    /**
     * TODO implement this function to give selection args for calendars {NOT_MVP}
     * This function will return the selection to only select events for today
     * Ultimately this will also filter according to shared prefs to only select
     * calendars that the user would like to have
     * @return a selection string to query the calendarProvider
     */
    //    private static String[] getSelectionArgs() {
    //        Pair dates = getDateTimes();
    //        return new String[]{getTimeInMillis((LocalDateTime)dates.first), getTimeInMillis((LocalDateTime)dates.second)};
    //    }

    /**
     * Function to get the LocalDateTime objects to define "Today"
     * TODO changed this to only start with current time, end time will still be tommorow midnight
     * @return a pair where the first is today midnight and second is tommorow midnight
     */
    private val dateTimes: Pair<LocalDateTime, LocalDateTime>
        get() {
            //val todayMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT)
            //val tommorowMidnight = todayMidnight.plusDays(1)
            val now = LocalDateTime.of(LocalDate.now(), LocalTime.now())
            return Pair(now, now.plusDays(1))
        }

    fun getCalendarEventsForToday(context: Context): List<Event> {
        val projection = arrayOf(CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.EVENT_ID)
        val times = dateTimes
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, getTimeInMillis(times.first))
        ContentUris.appendId(builder, getTimeInMillis(times.second))

        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            val cursor = context.contentResolver.query(
                    builder.build(),
                    projection, null, null,
                    CalendarContract.Instances.BEGIN + " ASC")
            convertToEventList(cursor, context)
        } else {
            ArrayList()
        }
    }

    private fun convertToEventList(cursor: Cursor?, context: Context): List<Event> {
        val eventList = ArrayList<Event>()
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndex(CalendarContract.Instances.END)
                val eventIdIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)

                val begin = cursor.getLong(beginIndex)
                val end = cursor.getLong(endIndex)
                val id = cursor.getInt(eventIdIndex).toString()
                eventList.add(getEvent(id, begin, end, context))
            }
        }
        return eventList
    }

    private fun getEvent(eventId: String, begin: Long, end: Long, context: Context): Event {
        val event = Event()
        event.startTime = Converters.dateTimeFromUnix(begin)
        event.endTime = Converters.dateTimeFromUnix(end)
        event.id = Integer.valueOf(eventId)
        event.watching = true
        event.distance = Constants.ROOM_INVALID_LONG_VALUE
        event.drivingTime = Constants.ROOM_INVALID_LONG_VALUE
        event.transportMode = Constants.TRANSPORT_DRIVING
        event.origin = ""
        val eventsCursor = getEventById(context, eventId)
        if (eventsCursor != null) {
            val titleIndex = eventsCursor.getColumnIndex(Constants.CALENDAR_EVENTS_TITLE)
            val descriptionIndex = eventsCursor.getColumnIndex(Constants.CALENDAR_EVENTS_DESCRIPTION)
            val locationIndex = eventsCursor.getColumnIndex(Constants.CALENDAR_EVENTS_EVENT_LOCATION)
            val calendarIdIndex = eventsCursor.getColumnIndex(Constants.CALENDAR_EVENTS_CALENDAR_ID)

            eventsCursor.moveToFirst()
            event.title = eventsCursor.getString(titleIndex)
            event.description = eventsCursor.getString(descriptionIndex)
            event.location = eventsCursor.getString(locationIndex)
            val latLng = convertLocationToLatLng(eventsCursor.getString(locationIndex))
            event.locationLatlng = latLng
            event.calendarId = eventsCursor.getLong(calendarIdIndex)
        }
        return event
    }

    //only run this on a background thread access dbs as well as other work will block UI
    @SuppressLint("MissingPermission")
    private fun getEventById(context: Context, id: String): Cursor? {
        val projection = arrayOf(BaseColumns._ID, Constants.CALENDAR_EVENTS_TITLE, Constants.CALENDAR_EVENTS_DESCRIPTION, Constants.CALENDAR_EVENTS_CALENDAR_ID, Constants.CALENDAR_EVENTS_EVENT_LOCATION)
        //TODO change this to only query calendars user has selected {NOT_MVP}
        //as of now filters for only events starting at midnight of that day until 11:59PM
        val selection = Constants.CALENDAR_EVENTS_ID + " = ?"

        return context.contentResolver.query(
                Constants.CALENDAR_EVENTS_URI,
                projection,
                selection,
                arrayOf(id), null)
    }

    private fun getTimeInMillis(dateTime: LocalDateTime): Long {
        val zdt = dateTime.atZone(ZoneId.systemDefault())
        return zdt.toInstant().toEpochMilli()
    }

    private fun convertCursorToEventList(cursor: Cursor?): List<Event> {
        val eventList = ArrayList<Event>()
        if (cursor != null) {
            while (cursor.moveToNext()) {
                eventList.add(getEvent(cursor))
            }
        }
        return eventList
    }

    /*
        Gets the current data from the cursor and converts it to an event
        Object
     */
    private fun getEvent(cursor: Cursor): Event {
        val idIndex = cursor.getColumnIndex(BaseColumns._ID)
        val titleIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_TITLE)
        val descriptionIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DESCRIPTION)
        val locationIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_EVENT_LOCATION)
        val calendarIdIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_CALENDAR_ID)
        val startIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DTSTART)
        val endIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DTEND)


        val event = Event()
        event.id = cursor.getInt(idIndex)
        event.title = cursor.getString(titleIndex)
        event.description = cursor.getString(descriptionIndex)
        event.location = cursor.getString(locationIndex)
        val latLng = convertLocationToLatLng(cursor.getString(locationIndex))
        event.locationLatlng = latLng
        event.startTime = Converters.dateTimeFromUnix(cursor.getLong(startIndex))
        event.endTime = Converters.dateTimeFromUnix(cursor.getLong(endIndex))
        event.calendarId = cursor.getLong(calendarIdIndex)
        event.watching = true
        event.distance = Constants.ROOM_INVALID_LONG_VALUE
        event.drivingTime = Constants.ROOM_INVALID_LONG_VALUE
        event.transportMode = Constants.TRANSPORT_DRIVING
        event.origin = ""
        return event
    }

    /**
     * Converts the Location String from the calendar to a LatLng object
     * @param location a string of the location ex. 153 east Broadway
     * @return LatLng of address provided
     */
    private fun convertLocationToLatLng(location: String?): LatLng? {
        return if (location == null || location == "") null else LocationUtils.latlngFromAddress(NeverLateApp.app, location)
    }

    /**
     * Function that compares two Event lists to see if there were changes, and if so check if
     * the change necessitates a new call to Distance Matrix
     * @param oldEventList the previous list
     * @param newEventList the new list
     * @return a Hashmap of Lists one called needsGeoChanged and another noGeoChange - events without
     * any change will be put in noGeoChange to make it easier when inserting events
     *
     * as sorting will not help and the lists will be off need to fix both adding an event to
     */
    fun compareCalendars(oldEventList: MutableList<Event>, newEventList: MutableList<Event>): HashMap<String, List<Event>> {
        val map = HashMap<String, List<Event>>()
        //filter out events from the old events that where removed in newEventList
        filterAndRemoveDeletedEvents(oldEventList, newEventList)
        //filter out any events in the new list that did not exist in old it will be
        //the base list to add geofences
        val eventsToAddWithGeofences = filterOutNewEvents(oldEventList, newEventList)
        val eventsToAddNoGeofences = ArrayList<Event>()

        //need to sort the lists by id rather by time so as for both to be in sync
        //in case a new event was added in a middle time-slot
        Collections.sort(oldEventList, Event.eventIdComparator)
        Collections.sort(newEventList, Event.eventIdComparator)

        // For each event check if it was changed and add it to the corresponding list
        // events with only a title or description change do not need new fences
        for (i in 0 until oldEventList.size) {
            val newEvent = newEventList[i]
            val oldEvent = oldEventList[i]
            if (oldEvent.drivingTime == Constants.ROOM_INVALID_LONG_VALUE && !newEvent.location.isEmpty()) {
                eventsToAddWithGeofences.add(newEvent)
            } else {
                val change = Event.eventChanged(oldEvent, newEvent)
                when (change) {
                    Event.Change.DESCRIPTION_CHANGE -> {
                        //add the old event as it contains the duration and distance data
                        oldEvent.title = newEvent.title
                        oldEvent.description = newEvent.description
                        eventsToAddNoGeofences.add(oldEvent)
                    }
                    Event.Change.GEOFENCE_CHANGE -> {
                        newEvent.watching = oldEvent.watching
                        //TODO add all other data that would not be in the new event
                        eventsToAddWithGeofences.add(newEvent)
                    }
                    Event.Change.SAME -> eventsToAddNoGeofences.add(oldEvent)
                }
            }
        }
        map[Constants.LIST_NEEDS_FENCE_UPDATE] = eventsToAddWithGeofences
        map[Constants.LIST_NO_FENCE_UPDATE] = eventsToAddNoGeofences

        return map
    }

    /**
     * will return an array of old events where all deleted events where removed
     * will also remove any geofences associated with them.
     * TODO both this function and the next would be made much easier by using java stream
     * which is not available below API 24
     * @return a list with all deleted events filtered out
     */
    private fun filterAndRemoveDeletedEvents(oldEvents: MutableList<Event>, newEvents: List<Event>) {
        val ids = mapToIds(newEvents)
        val toRemove = ArrayList<Event>()
        val creator = AwarenessFencesCreator.Builder(null).build()

        for (event in oldEvents) {
            if (!ids.contains(event.id)) {
                toRemove.add(event)
            }
        }
        oldEvents.removeAll(toRemove)
    }

    private fun filterOutNewEvents(oldEvents: List<Event>, newEvents: MutableList<Event>): MutableList<Event> {
        val toRemove = ArrayList<Event>()
        val ids = mapToIds(oldEvents)
        for (event in newEvents) {
            if (!ids.contains(event.id)) {
                toRemove.add(event)
            }
        }
        newEvents.removeAll(toRemove)
        return toRemove.filter {!it.location.isEmpty()}.toMutableList()
    }

    private fun mapToIds(eventList: List<Event>): List<Int> {
        val ids = ArrayList<Int>()
        for (event in eventList) {
            ids.add(event.id)
        }
        return ids
    }


}
