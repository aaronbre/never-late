package com.aaronbrecher.neverlate.backgroundservices;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

public class CalendarAlarmService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public CalendarAlarmService() {
        super("Calendar-alarm-service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
