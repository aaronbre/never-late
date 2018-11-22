package com.aaronbrecher.neverlate.Utils;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.backgroundservices.AnaylizeEventsJobService;
import com.aaronbrecher.neverlate.backgroundservices.CheckForCalendarChangedService;
import com.aaronbrecher.neverlate.backgroundservices.SetupActivityRecognitionJobService;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

public class BackgroundUtils {
    public static final int DEFAULT_JOB_TIMEFRAME = 15;
    public static final int ONE_HOUR_IN_SECONDS = 60 * 60;

    /**
     * Job to Setup the app to listen to activity changes this is done as a job to
     */
    public static Job setUpActivityRecognitionJob(FirebaseJobDispatcher dispatcher) {
        return dispatcher.newJobBuilder()
                .setService(SetupActivityRecognitionJobService.class)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setTag(Constants.FIREBASE_JOB_SERVICE_SETUP_ACTIVITY_RECOG)
                .setTrigger(Trigger.executionWindow(ONE_HOUR_IN_SECONDS * 23, ONE_HOUR_IN_SECONDS * 24))
                .setRecurring(true)
                .setReplaceCurrent(false)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
    }

    /**
     * Returns a Job that will be used to check the calendar for changes periodically
     */
    public static Job setUpPeriodicCalendarChecks(FirebaseJobDispatcher dispatcher){
        return dispatcher.newJobBuilder()
                .setService(CheckForCalendarChangedService.class)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setTag(Constants.FIREBASE_JOB_SERVICE_CHECK_CALENDAR_CHANGED)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(Constants.CHECK_CALENDAR_START_WINDOW, Constants.CHECK_CALENDAR_END_WINDOW))
                .setReplaceCurrent(false)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .build();
    }

    public static Job oneTimeCalendarUpdate(FirebaseJobDispatcher dispatcher){
        return dispatcher.newJobBuilder()
                .setService(CheckForCalendarChangedService.class)
                .setTag(Constants.FIREBASE_JOB_SERVICE_CHECK_CALENDAR_CHANGED_ONE_TIME)
                .setRecurring(false)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.NOW)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
    }

    public static Job anaylzeSchedule(FirebaseJobDispatcher dispatcher){
        return dispatcher.newJobBuilder()
                .setService(AnaylizeEventsJobService.class)
                .setTag(Constants.FIREBASE_JOB_SERVICE_ANALYZE_SCHEDULE_TAG)
                .setRecurring(false)
                .setTrigger(Trigger.NOW)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .build();
    }
}
