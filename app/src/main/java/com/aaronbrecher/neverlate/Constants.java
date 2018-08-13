package com.aaronbrecher.neverlate;

import android.net.Uri;
import android.provider.CalendarContract.Events;

public class Constants {
    public static final String DATABASE_NAME = "events-database";
    public static final int PERMISSIONS_REQUEST_CODE = 101;
    public static final String MILES_PER_MINUTE_PREFS_KEY = "miles-per-minute";
    public static final int GEOFENCE_TRANSITION_PENDING_INTENT_CODE = 0;
    public static final String NOTIFICATION_CHANNEL_ID = "geofencing-channel";
    public static final String EVENT_DETAIL_INTENT_EXTRA = "event-details";
    public static final String EVENT_LIST_TAG = "event-list-frag";
    public static final String EVENT_DETAIL_FRAGMENT_TAG = "event-detail-frag";
    public static final String GEOFENCE_REQUEST_ID = "never-late-fences";
    public static final int GEOFENCE_RESPONSE_MILLIS = 100000;

    //calendar constants
    public static final String CALENDAR_EVENTS_TITLE = Events.TITLE;
    public static final String CALENDAR_EVENTS_DESCRIPTION = Events.DESCRIPTION;
    public static final String CALENDAR_EVENTS_CALENDAR_ID = Events.CALENDAR_ID;
    public static final String CALENDAR_EVENTS_EVENT_LOCATION = Events.EVENT_LOCATION;
    public static final String CALENDAR_EVENTS_DTSTART = Events.DTSTART;
    public static final String CALENDAR_EVENTS_DTEND = Events.DTEND;
    public static final Uri CALENDAR_EVENTS_URI = Events.CONTENT_URI;;


}
