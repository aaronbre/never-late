package com.aaronbrecher.neverlate.backgroundservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;

public class BootCompletedBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)){
            boolean wasSet = BackgroundUtils.setAlarmManager(context);
            if(wasSet){
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit().putBoolean(Constants.ALARM_STATUS_KEY, true)
                        .apply();
            }
        }
    }
}
