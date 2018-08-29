package com.aaronbrecher.neverlate.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.backgroundservices.CalendarAlarmService;
import com.aaronbrecher.neverlate.backgroundservices.GeofenceJobService;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

public class BackgroundUtils {
    public static final int DEFAULT_JOB_TIMEFRAME = 15;

    /**
     * Helper function to set up an AlarmManager to be used to sync calendar
     *
     * @param context context to be used to set up alarm and intents
     * @return boolean if alarm was set will be true
     */
    public static boolean setAlarmManager(Context context) {
        //get the time of midnight today will be the initial trigger
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        ZonedDateTime zdt = midnight.atZone(ZoneId.systemDefault());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CalendarAlarmService.class);
        intent.setAction(Constants.ACTION_ADD_CALENDAR_EVENTS);
        PendingIntent pendingIntent = PendingIntent.getService(context, Constants.CALENDAR_ALARM_SERVICE_REQUEST_CODE, intent, 0);
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC,
                    zdt.toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pendingIntent);
            return true;
        }
        return false;
    }

    /**
     * Create a job to repeat every 15 minutes to update the geofences ultimately
     * will either use the default 15 mins or user provided time from prefs
     * @param dispatcher a dispatcher to create the job
     * @param timeframe the timeframe to schedule job, end will be +5
     * @return a FirebaseJob to schedule via firebase dispatcher
     */
    public static Job createJob(FirebaseJobDispatcher dispatcher, int timeframe) {
        //TODO change trigger to check for user preference...
        return dispatcher.newJobBuilder()
                .setService(GeofenceJobService.class)
                .setTag(Constants.FIREBASE_JOB_SERVICE_UPDATE_GEOFENCES)
                .setTrigger(Trigger.executionWindow(timeframe * 60, (timeframe + 5) * 60))
                .setRecurring(true)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
    }
}
