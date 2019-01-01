package com.aaronbrecher.neverlate.backgroundservices

import android.app.PendingIntent
import android.content.Intent

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers.StartJobIntentServiceBroadcastReceiver
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

import java.util.ArrayList

/**
 * This jobService will set up the Activity recognition, this is being done in a job service
 * rather then hardcoded so as to make use of the reschedule in the event that the requestActivity
 * fails.
 */
class SetupActivityRecognitionJobService : JobService() {

    // this pending intent uses the app context so a different pending intent will have the same context
    // this will be useful for removing the ActivityReccognition in a different class...
    private val pendingIntent: PendingIntent
        get() {
            val intent = Intent(applicationContext, StartJobIntentServiceBroadcastReceiver::class.java)
            intent.action = Constants.ACTION_START_ACTIVITY_TRANSITION_SERVICE
            return PendingIntent.getBroadcast(this, Constants.ACTIVITY_TRANSITION_PENDING_INTENT_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    override fun onStartJob(job: JobParameters): Boolean {
        Thread { doWork(job) }.start()
        return true
    }

    override fun onStopJob(job: JobParameters): Boolean {
        return true
    }

    /**
     * The job to be run on a separate thread this will set up the app to monitor if the user
     * has begun driving or if they have stopped driving
     *
     * @param job the job params to be used to call jobFinished
     */
    private fun doWork(job: JobParameters) {
        val transitions = ArrayList<ActivityTransition>()
        transitions.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build())
        transitions.add(ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build())
        val request = ActivityTransitionRequest(transitions)
        val pendingIntent = pendingIntent

        ActivityRecognition.getClient(applicationContext).requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener { jobFinished(job, false) }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    jobFinished(job, true)
                }
    }
}
