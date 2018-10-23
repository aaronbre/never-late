package com.aaronbrecher.neverlate.Utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.CalendarContract.Events;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.models.Event;
import com.google.android.gms.maps.model.LatLng;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;


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
                + Constants.CALENDAR_EVENTS_DTSTART + " <= ? AND "
                //+ Constants.CALENDAR_EVENTS_EVENT_LOCATION + " IS NOT NULL AND " + Constants.CALENDAR_EVENTS_EVENT_LOCATION + " != '' AND "
                + "(deleted != 1)";
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
        return new String[]{getTimeInMillis((LocalDateTime) dates.first), getTimeInMillis((LocalDateTime) dates.second)};
    }

    /**
     * Function to get the LocalDateTime objects to define "Today"
     * @return a pair where the first is today midnight and second is tommorow midnight
     */
    private static Pair<LocalDateTime, LocalDateTime> getDateTimes(){
        LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        LocalDateTime tommorowMidnight = todayMidnight.plusDays(1);
        return new Pair<>(todayMidnight, tommorowMidnight);
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
        event.setTimeTo(Constants.ROOM_INVALID_LONG_VALUE);
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


}
