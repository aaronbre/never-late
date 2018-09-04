package com.aaronbrecher.neverlate.backgroundservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;

import java.util.List;

public class ActivityTransitionService extends JobIntentService {
    static int JOB_ID = 1001;

    static void enqueueWork(Context context, Intent work){
        enqueueWork(context, ActivityTransitionService.class, JOB_ID, work);
    }
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if(ActivityTransitionResult.hasResult(intent)){
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            //for our purposes only the last event should be considered, if user already parked previous data
            //is not relevant and vice versa
            List<ActivityTransitionEvent> events = result.getTransitionEvents();
            ActivityTransitionEvent event = events.get(events.size()-1);
            if(event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT){
                setUpFences();
            } else if(event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER){
                setUpDrivingService();
            }
        }
    }

    private void setUpDrivingService() {
        //TODO either set up a jobservice for a specified amount of time or locationUpdateRequest
    }

    private void setUpFences() {
        //TODO set up the fences using the new location
    }
}
