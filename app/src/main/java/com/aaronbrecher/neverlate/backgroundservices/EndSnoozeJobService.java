package com.aaronbrecher.neverlate.backgroundservices;

import android.preference.PreferenceManager;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class EndSnoozeJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters job) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        dispatcher.mustSchedule(BackgroundUtils.setUpActivityRecognitionJob(dispatcher));
        dispatcher.mustSchedule(BackgroundUtils.oneTimeCalendarUpdate(dispatcher));
        dispatcher.mustSchedule(BackgroundUtils.setUpPeriodicCalendarChecks(dispatcher));
        PreferenceManager.getDefaultSharedPreferences(this).edit().putLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE).commit();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
