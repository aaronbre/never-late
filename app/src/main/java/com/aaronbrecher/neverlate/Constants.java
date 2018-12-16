package com.aaronbrecher.neverlate;

import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract.Events;

public class Constants {
    public static final String DATABASE_NAME = "events-database";
    public static final int PERMISSIONS_REQUEST_CODE = 101;
    public static final String NOTIFICATION_CHANNEL_ID = "geofencing-channel";
    public static final String EVENT_DETAIL_INTENT_EXTRA = "event-details";
    public static final String EVENT_LIST_TAG = "event-list-frag";
    public static final String EVENT_DETAIL_FRAGMENT_TAG = "event-detail-frag";
    public static final String GEOFENCE_REQUEST_ID = "never-late-fences";

    //awareness constants
    public static final int AWARENESS_TRANSITION_PENDING_INTENT_CODE = 1;
    public static final String AWARENESS_FENCE_PREFIX = "never-late-awareness";
    public static final String AWARENESS_FENCE_MAIN_PREFIX = "never-late-awareness-main";
    public static final String AWARENESS_FENCE_END_PREFIX = "never-late-awareness-end";
    public static final String AWARENESS_FENCE_ARRIVAL_PREFIX = "never-late-awareness-arrived";
    public static final long TIME_FIVE_MINUTES = 5 * 60 * 1000;
    public static final long TIME_TEN_MINUTES = TIME_FIVE_MINUTES * 2;
    public static final long TIME_FIFTEEN_MINUTES = TIME_FIVE_MINUTES * 3;
    public static final long ONE_HOUR = TIME_TEN_MINUTES * 6;
    public static final long ONE_DAY = ONE_HOUR * 24;
    public static final long FIVE_HOUR = ONE_HOUR * 5;
    public static final long LOCATION_FENCE_RADIUS = 1000;
    public static final long LOCATION_FENCE_DWELL_TIME = 10 * 1000;
    public static final long ARRIVAL_FENCE_RADIUS = 200;
    public static final long ARRIVAL_FENCE_DWELL_TIME = LOCATION_FENCE_DWELL_TIME;

    //calendar constants
    public static final String CALENDAR_EVENTS_TITLE = Events.TITLE;
    public static final String CALENDAR_EVENTS_DESCRIPTION = Events.DESCRIPTION;
    public static final String CALENDAR_EVENTS_CALENDAR_ID = Events.CALENDAR_ID;
    public static final String CALENDAR_EVENTS_EVENT_LOCATION = Events.EVENT_LOCATION;
    public static final String CALENDAR_EVENTS_DTSTART = Events.DTSTART;
    public static final String CALENDAR_EVENTS_DTEND = Events.DTEND;
    public static final Uri CALENDAR_EVENTS_URI = Events.CONTENT_URI;
    public static final String CALENDAR_EVENTS_ID = Events._ID;

    public static final String LIST_NEEDS_FENCE_UPDATE = "needs-fence-update";
    public static final String LIST_NO_FENCE_UPDATE = "no-fence-update";

    //Services Constants
    public static final String FIREBASE_JOB_SERVICE_SETUP_ACTIVITY_RECOG = "set-up-activity-recog";
    public static final int CALENDAR_ALARM_SERVICE_REQUEST_CODE = 102;
    public static final int ACTIVITY_TRANSITION_PENDING_INTENT_CODE = 2;
    public static final String FIREBASE_JOB_SERVICE_CHECK_CALENDAR_CHANGED = "check-calendar-change";
    public static final String FIREBASE_JOB_SERVICE_CHECK_CALENDAR_CHANGED_ONE_TIME = "check-calendar-change-one-time";
    public static final int CHECK_CALENDAR_START_WINDOW = 15*60;
    public static final int CHECK_CALENDAR_END_WINDOW = CHECK_CALENDAR_START_WINDOW + 120;

    //prefs key
    public static final String ALERTS_PREFS_KEY = "alert-time";
    public static final String ALERT_TIME_SHORT = "5";
    public static final String ALERT_TIME_MEDIUM = "10";
    public static final String ALERT_TIME_LONG = "15";
    public static final String SNOOZE_PREFS_KEY = "snooze-time";
    public static final String SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY = "snooze-type";
    public static final String DISABLED_DRIVING_EVENTS = "disabled-driving";
    public static final String CALENDARS_PREFS_KEY = "calendars";



    public static final int UNIT_SYSTEM_METRIC = 1;
    public static final int UNIT_SYSTEM_IMPERIAL = 2;

    //actions
    public static final String ACTION_ADD_CALENDAR_EVENTS = "com.aaronbrecher.neverlate.action.RETRIEVE_CALENDAR_EVENTS";
    public static final String ACTION_START_AWARENESS_FENCE_SERVICE = "com.aaronbrecher.neverlate.action.START_AWARENESS_FENCE_SERVICE";
    public static final String ACTION_START_ACTIVITY_TRANSITION_SERVICE = "com.aaronbrecher.neverlate.action.START_ACTIVITY_TRANSITION_SERVICE";
    public static final String ACTION_PROCESS_LOCATION_UPDATE = "com.aaronbrecher.neverlate.action.TRACK_CURRENT_LOCATION";

    public static final long ROOM_INVALID_LONG_VALUE = -1L;
    public static final int TRANSPORT_DRIVING = 1;
    public static final int TRANSPORT_WALKING = 2;
    public static final int TRANSPORT_PUBLIC = 3;
    public static final String ACTION_CANCEL_DRIVING_SERVICE = "com.aaronbrecher.neverlate.action.CANCEL_DRIVING_SERVICE";

    public static final Uri PRIVACY_POLICY_URI = Uri.parse("https://never-late-api.herokuapp.com/privacy-policy");
    public static final String FIREBASE_JOB_SERVICE_ANALYZE_SCHEDULE_TAG = "analyze-schedule";
    public static final String FIREBASE_JOB_SERVICE_END_SNOOZE = "end-snooze";
}
