package com.aaronbrecher.neverlate.backgroundservices;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;

public class BootCompletedJobService extends JobIntentService {
    public static final int JOB_ID = 1004;

    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, BootCompletedJobService.class, JOB_ID, work);
    }
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        boolean wasSet = BackgroundUtils.setAlarmManager(this);
        if(wasSet){
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putBoolean(Constants.ALARM_STATUS_KEY, true)
                    .apply();
        }
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job job = BackgroundUtils.setUpActivityRecognitionJob(dispatcher);
        dispatcher.mustSchedule(job);
    }
}
