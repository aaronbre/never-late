package com.aaronbrecher.neverlate.backgroundservices.jobintentservices;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import android.util.Log;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;

public class BootCompletedJobService extends JobIntentService {
    public static final int JOB_ID = 1004;
    private static final String TAG = BootCompletedJobService.class.getSimpleName();

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, BootCompletedJobService.class, JOB_ID, work);
    }
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(TAG, "onHandleWork");
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(NeverLateApp.getApp()));
        //one time refresh of the calendar
        dispatcher.mustSchedule(BackgroundUtils.oneTimeCalendarUpdate(dispatcher));
        //reset the periodic calendar update
        dispatcher.mustSchedule(BackgroundUtils.setUpPeriodicCalendarChecks(dispatcher));
        //reset the activity recog job
        dispatcher.mustSchedule(BackgroundUtils.setUpActivityRecognitionJob(dispatcher));
    }
}
