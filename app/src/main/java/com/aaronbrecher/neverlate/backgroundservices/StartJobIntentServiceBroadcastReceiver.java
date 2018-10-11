package com.aaronbrecher.neverlate.backgroundservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.aaronbrecher.neverlate.Constants;

public class StartJobIntentServiceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "NeverLateBroadcast";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: was called");
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case Constants.ACTION_ADD_CALENDAR_EVENTS:
                CalendarAlarmService.enqueueWork(context, intent);
                break;
            case Constants.ACTION_START_AWARENESS_FENCE_SERVICE:
                AwarenessFenceTransitionService.enqueueWork(context, intent);
                break;
            case Constants.ACTION_START_ACTIVITY_TRANSITION_SERVICE:
                ActivityTransitionService.enqueueWork(context, intent);
                break;
        }
    }
}
