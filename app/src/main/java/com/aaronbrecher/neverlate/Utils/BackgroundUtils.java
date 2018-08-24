package com.aaronbrecher.neverlate.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.backgroundservices.CalendarAlarmService;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

public class BackgroundUtils {

    /**
     * Helper function to set up an AlarmManager to be used to sync calendar
     * @param context context to be used to set up alarm and intents
     * @return boolean if alarm was set will be true
     */
    public static boolean setAlarmManager(Context context){
        //get the time of midnight today will be the initial trigger
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        ZonedDateTime zdt = midnight.atZone(ZoneId.systemDefault());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CalendarAlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, Constants.CALENDAR_ALARM_SERVICE_REQUEST_CODE, intent, 0);
        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC,
                    zdt.toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pendingIntent);
            return true;
        }
        return false;
    }
}
