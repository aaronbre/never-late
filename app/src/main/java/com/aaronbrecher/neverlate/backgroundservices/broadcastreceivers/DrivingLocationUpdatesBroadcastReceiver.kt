package com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location

import com.aaronbrecher.neverlate.Constants
import com.google.android.gms.location.LocationResult

class DrivingLocationUpdatesBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (Constants.ACTION_PROCESS_LOCATION_UPDATE == action) {
                val result = LocationResult.extractResult(intent)
                if (result != null) {
                    val location = result.lastLocation
                    val helper = DrivingLocationHelper(location, context)
                    helper.checkAllEvents()
                }
            }
        }
    }
}
