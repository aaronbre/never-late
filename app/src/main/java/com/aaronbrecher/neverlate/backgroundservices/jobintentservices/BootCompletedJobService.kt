package com.aaronbrecher.neverlate.backgroundservices.jobintentservices

import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import androidx.core.app.JobIntentService
import android.util.Log

import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.Utils.BackgroundUtils
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver

class BootCompletedJobService : JobIntentService() {
    override fun onHandleWork(@NonNull intent: Intent) {
        Log.i(TAG, "onHandleWork")
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(NeverLateApp.app))
        //one time refresh of the calendar
        dispatcher.mustSchedule(BackgroundUtils.oneTimeCalendarUpdate(dispatcher))
        //reset the periodic calendar update
        dispatcher.mustSchedule(BackgroundUtils.setUpPeriodicCalendarChecks(dispatcher))
        //reset the activity recog job
        dispatcher.mustSchedule(BackgroundUtils.setUpActivityRecognitionJob(dispatcher))
    }

    companion object {
        val JOB_ID = 1004
        private val TAG = BootCompletedJobService::class.java.simpleName

        fun enqueueWork(context: Context, work: Intent) {
            JobIntentService.enqueueWork(context, BootCompletedJobService::class.java, JOB_ID, work)
        }
    }
}
