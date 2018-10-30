package com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.aaronbrecher.neverlate.Constants;
import com.google.android.gms.location.LocationResult;

public class DrivingLocationUpdatesBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Constants.ACTION_PROCESS_LOCATION_UPDATE.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    Location location = result.getLastLocation();
                    DrivingLocationHelper helper = new DrivingLocationHelper(location, context);
                    helper.checkAllEvents();
                }
            }
        }
    }
}
