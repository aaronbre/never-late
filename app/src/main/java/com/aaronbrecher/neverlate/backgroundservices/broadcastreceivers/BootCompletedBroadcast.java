package com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aaronbrecher.neverlate.backgroundservices.jobintentservices.BootCompletedJobService;

/**
 * code to be run on device reboot the jobservice will reset all recurring jobs
 * as well as do an immediate resync with the calendar
 * WILL also be run when the app updates
 */
public class BootCompletedBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)){
            BootCompletedJobService.enqueueWork(context, intent);
        }
    }
}
