package com.aaronbrecher.neverlate.CalendarUtils;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.zone.ZoneRules;

import java.util.TimeZone;

import javax.inject.Inject;


public class CalendarUtils {
    public static final Uri CALENDAR_URI = Events.CONTENT_URI;

    @Inject
    SharedPreferences mSharedPreferences;

    public static Cursor getCalendarEventsForToday(Context context){
        String[] projection = new String[]{BaseColumns._ID,
                Events.TITLE,
                Events.DESCRIPTION,
                Events.CALENDAR_ID,
                Events.DTSTART,
                Events.DTEND,
                Events.EVENT_LOCATION};
        //TODO change this to only query calendars user has selected {NOT_MVP}
        String selection = Events.DTSTART + " >= ? AND " + Events.DTSTART + " <= ?";
        String[] args = getSelectionArgs();
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED){
            return context.getContentResolver().query(
                    CALENDAR_URI,
                    projection,
                    selection,
                    getSelectionArgs(),
                    Events.DTSTART + " ASC");
        }
        else return null;
    }

    /**
     * TODO implement this function to give selection args for calendars and dates {NOT_MVP}
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
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate today = LocalDate.now();
        LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);
        LocalDateTime tommorowMidnight = todayMidnight.plusDays(1);
        return new Pair<>(todayMidnight, tommorowMidnight);
    }

    private static String getTimeInMillis(LocalDateTime dateTime){
        ZonedDateTime zdt = dateTime.atZone(ZoneId.of(TimeZone.getDefault().getID()));
        return String.valueOf(zdt.toInstant().toEpochMilli());
    }
}
