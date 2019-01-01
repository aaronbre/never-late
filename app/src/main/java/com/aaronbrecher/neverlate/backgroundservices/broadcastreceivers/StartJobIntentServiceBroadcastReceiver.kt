package com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.backgroundservices.jobintentservices.ActivityTransitionService
import com.aaronbrecher.neverlate.backgroundservices.jobintentservices.AwarenessFenceTransitionService

class StartJobIntentServiceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: was called" + action!!)
        if (action == null) return
        when (action) {
            Constants.ACTION_START_AWARENESS_FENCE_SERVICE -> {
                Log.i(TAG, "onReceive: awareness-transition-triggered")
                AwarenessFenceTransitionService.enqueueWork(context, intent)
            }
            Constants.ACTION_START_ACTIVITY_TRANSITION_SERVICE -> {
                Log.i(TAG, "onReceive: activity-transition-triggered")
                ActivityTransitionService.enqueueWork(context, intent)
            }
        }
    }

    companion object {
        private val TAG = "NeverLateBroadcast"
    }
}
