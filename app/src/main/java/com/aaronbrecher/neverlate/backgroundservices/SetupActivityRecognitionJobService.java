package com.aaronbrecher.neverlate.backgroundservices;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.aaronbrecher.neverlate.Constants;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This jobService will set up the Activity recognition, this is being done in a job service
 * rather then hardcoded so as to make use of the reschedule in the event that the requestActivity
 * fails.
 */
public class SetupActivityRecognitionJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters job) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doWork(job);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return true;
    }

    /**
     * The job to be run on a separate thread this will set up the app to monitor if the user
     * has begun driving or if they have stopped driving
     * @param job
     */
    public void doWork(final JobParameters job) {
        List<ActivityTransition> transitions = new ArrayList<>();
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);
        PendingIntent pendingIntent = getPendingIntent();

        ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        jobFinished(job, false);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                jobFinished(job, true);
            }
        });
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, StartJobIntentServiceBroadcastReceiver.class);
        intent.setAction(Constants.ACTION_START_ACTIVITY_TRANSITION_SERVICE);
        return PendingIntent.getBroadcast(this, Constants.ACTIVITY_TRANSITION_PENDING_INTENT_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}