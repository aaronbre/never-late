package com.aaronbrecher.neverlate;

import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract.Events;

public class Constants {
    public static final String DATABASE_NAME = "events-database";
    public static final int PERMISSIONS_REQUEST_CODE = 101;
    public static final int GEOFENCE_TRANSITION_PENDING_INTENT_CODE = 0;
    public static final String NOTIFICATION_CHANNEL_ID = "geofencing-channel";
    public static final String EVENT_DETAIL_INTENT_EXTRA = "event-details";
    public static final String EVENT_LIST_TAG = "event-list-frag";
    public static final String EVENT_DETAIL_FRAGMENT_TAG = "event-detail-frag";
    public static final String GEOFENCE_REQUEST_ID = "never-late-fences";
    public static final int GEOFENCE_RESPONSE_MILLIS = 100000;

    //awareness constants
    public static final int AWARENESS_TRANSITION_PENDING_INTENT_CODE = 1;
    public static final String AWARENESS_FENCE_NAME_PREFIX = "never-late-awareness";
    public static final long TIME_TEN_MINUTES = 10 * 60 * 1000;
    public static final long ONE_HOUR = TIME_TEN_MINUTES * 6;
    public static final long LOCATION_FENCE_RADIUS = 2000;
    public static final long LOCATION_FENCE_DWELL_TIME = 10 * 1000;
    public static final String USER_LOCATION_PREFS_KEY = "users-location";

    //calendar constants
    public static final String CALENDAR_EVENTS_TITLE = Events.TITLE;
    public static final String CALENDAR_EVENTS_DESCRIPTION = Events.DESCRIPTION;
    public static final String CALENDAR_EVENTS_CALENDAR_ID = Events.CALENDAR_ID;
    public static final String CALENDAR_EVENTS_EVENT_LOCATION = Events.EVENT_LOCATION;
    public static final String CALENDAR_EVENTS_DTSTART = Events.DTSTART;
    public static final String CALENDAR_EVENTS_DTEND = Events.DTEND;
    public static final Uri CALENDAR_EVENTS_URI = Events.CONTENT_URI;
    ;

    //Services Constants
    public static final String FIREBASE_JOB_SERVICE_UPDATE_GEOFENCES = "update-geofences";
    public static final String FIREBASE_JOB_SERVICE_SETUP_ACTIVITY_RECOG = "set-up-activity-recog";
    public static final int CALENDAR_ALARM_SERVICE_REQUEST_CODE = 102;
    public static final int ACTIVITY_TRANSITION_PENDING_INTENT_CODE = 2;

    //prefs key
    public static final String KM_PER_MINUTE_PREFS_KEY = "miles-per-minute";
    public static final String UNIT_SYSTEM_PREFS_KEY = "unit-system";
    public static final String ALARM_STATUS_KEY = "alarm-status";



    public static final int UNIT_SYSTEM_METRIC = 1;
    public static final int UNIT_SYSTEM_IMPERIAL = 2;

    public static final String ACTION_ADD_CALENDAR_EVENTS = "com.aaronbrecher.neverlate.action.RETRIEVE_CALENDAR_EVENTS";
    public static final String ACTION_START_AWARENESS_FENCE_SERVICE = "com.aaronbrecher.neverlate.action.START_AWARENESS_FENCE_SERVICE";
    public static final String ACTION_START_ACTIVITY_TRANSITION_SERVICE = "com.aaronbrecher.neverlate.action.START_ACTIVITY_TRANSITION_SERVICE";

    public static final long ROOM_INVALID_LONG_VALUE = -1;
}
