package com.aaronbrecher.neverlate.Utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.maps.model.LatLng;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class CalendarUtils {

    //only run this on a background thread access dbs as well as other work will block UI
    public static List<Event> getCalendarEventsForToday(Context context){
        String[] projection = new String[]{BaseColumns._ID,
                Constants.CALENDAR_EVENTS_TITLE,
                Constants.CALENDAR_EVENTS_DESCRIPTION,
                Constants.CALENDAR_EVENTS_CALENDAR_ID,
                Constants.CALENDAR_EVENTS_DTSTART,
                Constants.CALENDAR_EVENTS_DTEND,
                Constants.CALENDAR_EVENTS_EVENT_LOCATION};
        //TODO change this to only query calendars user has selected {NOT_MVP}
        //as of now filters for only events starting at midnight of that day until 11:59PM
        String selection = Constants.CALENDAR_EVENTS_DTSTART + " >= ? AND "
                + Constants.CALENDAR_EVENTS_DTSTART + " <= ? AND " + "(deleted != 1)";
        String[] args = getSelectionArgs();
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED){
            Cursor calendarCursor = context.getContentResolver().query(
                    Constants.CALENDAR_EVENTS_URI,
                    projection,
                    selection,
                    getSelectionArgs(),
                    Constants.CALENDAR_EVENTS_DTSTART + " ASC");
            return convertCursorToEventList(calendarCursor);
        }
        else return new ArrayList<>();
    }

    /**
     * TODO implement this function to give selection args for calendars {NOT_MVP}
     * This function will return the selection to only select events for today
     * Ultimately this will also filter according to shared prefs to only select
     * calendars that the user would like to have
     * @return a selection string to query the calendarProvider
     */
    private static String[] getSelectionArgs() {
        Pair dates = getDateTimes();
        return new String[]{getTimeInMillis((LocalDateTime)dates.first), getTimeInMillis((LocalDateTime)dates.second)};
    }

    /**
     * Function to get the LocalDateTime objects to define "Today"
     * TODO changed this to only start with current time, end time will still be tommorow midnight
     * @return a pair where the first is today midnight and second is tommorow midnight
     */
    private static Pair<LocalDateTime, LocalDateTime> getDateTimes(){
        LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        LocalDateTime tommorowMidnight = todayMidnight.plusDays(1);
        return new Pair<>(LocalDateTime.of(LocalDate.now(), LocalTime.now()), tommorowMidnight);
    }

    private static String getTimeInMillis(LocalDateTime dateTime){
        ZonedDateTime zdt = dateTime.atZone(ZoneId.systemDefault());
        return String.valueOf(zdt.toInstant().toEpochMilli());
    }

    private static List<Event> convertCursorToEventList(Cursor cursor){
        List<Event> eventList = new ArrayList<>();
        if(cursor != null){
            while (cursor.moveToNext()){
                eventList.add(getEvent(cursor));
            }
        }
        return eventList;
    }

    /*
        Gets the current data from the cursor and converts it to an event
        Object
     */
    private static Event getEvent(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(BaseColumns._ID);
        int titleIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_TITLE);
        int descriptionIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DESCRIPTION);
        int locationIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_EVENT_LOCATION);
        int startIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DTSTART);
        int endIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DTEND);
        int calendarIdIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_CALENDAR_ID);

        Event event = new Event();
        event.setId(cursor.getInt(idIndex));
        event.setTitle(cursor.getString(titleIndex));
        event.setDescription(cursor.getString(descriptionIndex));
        event.setLocation(cursor.getString(locationIndex));
        LatLng latLng = convertLocationToLatLng(cursor.getString(locationIndex));
        event.setLocationLatlng(latLng);
        event.setStartTime(Converters.dateTimeFromUnix(cursor.getLong(startIndex)));
        event.setEndTime(Converters.dateTimeFromUnix(cursor.getLong(endIndex)));
        event.setCalendarId(cursor.getLong(calendarIdIndex));
        event.setWatching(true);
        event.setDistance(Constants.ROOM_INVALID_LONG_VALUE);
        event.setDrivingTime(Constants.ROOM_INVALID_LONG_VALUE);
        event.setTransportMode(Constants.TRANSPORT_DRIVING);
        event.setOrigin("");
        return event;
    }

    /**
     * Converts the Location String from the calendar to a LatLng object
     * @param location a string of the location ex. 153 east Broadway
     * @return LatLng of address provided
     */
    private static LatLng convertLocationToLatLng(String location){
        if(location == null || location.equals("")) return null;
        return LocationUtils.latlngFromAddress(NeverLateApp.getApp(), location);
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
    public static HashMap<String, List<Event>> compareCalendars(List<Event> oldEventList, List<Event> newEventList){
        HashMap<String, List<Event>> map = new HashMap<>();
        //filter out events from the old events that where removed in newEventList
        filterAndRemoveDeletedEvents(oldEventList, newEventList);
        //filter out any events in the new list that did not exist in old it will be
        //the base list to add geofences
        List<Event> eventsToAddWithGeofences = filterOutNewEvents(oldEventList, newEventList);
        List<Event> eventsToAddNoGeofences = new ArrayList<>();

        //need to sort the lists by id rather by time so as for both to be in sync
        //in case a new event was added in a middle time-slot
        Collections.sort(oldEventList, Event.eventIdComparator);
        Collections.sort(newEventList, Event.eventIdComparator);

        // For each event check if it was changed and add it to the corresponding list
        // events with only a title or description change do not need new fences
        for (int i = 0, listLength = oldEventList.size(); i < listLength; i++) {
            Event newEvent = newEventList.get(i);
            Event oldEvent = oldEventList.get(i);
            if(oldEvent.getDrivingTime() == Constants.ROOM_INVALID_LONG_VALUE){
                eventsToAddWithGeofences.add(newEvent);
            }
            else{
                Event.Change change = Event.eventChanged(oldEvent, newEvent);
                switch (change) {
                    case DESCRIPTION_CHANGE:
                        //add the old event as it contains the duration and distance data
                        oldEvent.setTitle(newEvent.getTitle());
                        oldEvent.setDescription(newEvent.getDescription());
                        eventsToAddNoGeofences.add(oldEvent);
                        break;
                    case GEOFENCE_CHANGE:
                        newEvent.setWatching(oldEvent.isWatching());
                        //TODO add all other data that would not be in the new event
                        eventsToAddWithGeofences.add(newEvent);
                        break;
                    case SAME:
                        eventsToAddNoGeofences.add(oldEvent);
                }
            }
        }
        map.put(Constants.LIST_NEEDS_FENCE_UPDATE, eventsToAddWithGeofences);
        map.put(Constants.LIST_NO_FENCE_UPDATE, eventsToAddNoGeofences);

        return map;
    }

    /**
     * will return an array of old events where all deleted events where removed
     * will also remove any geofences associated with them.
     * TODO both this function and the next would be made much easier by using java stream
     * which is not available below API 24
     * @return a list with all deleted events filtered out
     */
    private static void filterAndRemoveDeletedEvents(List<Event> oldEvents, List<Event> newEvents){
        List<Integer> ids = mapToIds(newEvents);
        List<Event> toRemove = new ArrayList<>();
        AwarenessFencesCreator creator = new AwarenessFencesCreator.Builder(null).build();

        for(Event event : oldEvents){
            if(!ids.contains(event.getId())){
               toRemove.add(event);
            }
        }
        oldEvents.removeAll(toRemove);
    }

    private static List<Event> filterOutNewEvents(List<Event> oldEvents, List<Event> newEvents){
        List<Event> toRemove = new ArrayList<>();
        List<Integer> ids = mapToIds(oldEvents);

        for(Event event : newEvents){
            if(!ids.contains(event.getId())){
                toRemove.add(event);
            }
        }
        newEvents.removeAll(toRemove);
        return toRemove;
    }

    private static List<Integer> mapToIds(List<Event> eventList){
        List<Integer> ids = new ArrayList<>();
        for(Event event : eventList){
            ids.add(event.getId());
        }
        return ids;
    }


}
