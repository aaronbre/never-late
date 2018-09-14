package com.aaronbrecher.neverlate.Utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.backgroundservices.StartJobIntentServiceBroadcastReceiver;
import com.aaronbrecher.neverlate.backgroundservices.SetupActivityRecognitionJobService;
import com.aaronbrecher.neverlate.interfaces.LocationCallback;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

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
        Intent intent = new Intent(context, StartJobIntentServiceBroadcastReceiver.class);
        intent.setAction(Constants.ACTION_ADD_CALENDAR_EVENTS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Constants.CALENDAR_ALARM_SERVICE_REQUEST_CODE, intent, 0);
        //TODO set to 15 for testing change interval to day
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC,
                    zdt.toInstant().toEpochMilli(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
            return true;
        }
        return false;
    }

    /**
     * Job to Setup the app to listen to activity changes this is done as a job to allow
     * for rescheduling in the event of the request failing
     */
    public static Job setUpActivityRecognitionJob(FirebaseJobDispatcher dispatcher) {
        return dispatcher.newJobBuilder()
                .setService(SetupActivityRecognitionJobService.class)
                .setTag(Constants.FIREBASE_JOB_SERVICE_SETUP_ACTIVITY_RECOG)
                .setTrigger(Trigger.NOW)
                .setRecurring(false)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
    }

    public static void getLocation(final LocationCallback callback, Context context, FusedLocationProviderClient providerClient) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        providerClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location == null) callback.getLocationFailedCallback();
                else callback.getLocationSuccessCallback(location);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.getLocationFailedCallback();
            }
        });
    }
}
