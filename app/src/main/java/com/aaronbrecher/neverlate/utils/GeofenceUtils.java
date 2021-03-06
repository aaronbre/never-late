package com.aaronbrecher.neverlate.utils;

import com.aaronbrecher.neverlate.database.Converters;

import org.threeten.bp.LocalDateTime;

public class GeofenceUtils {
    private static final String TAG = GeofenceUtils.class.getSimpleName();



    /**
     * Determine time to use to calculate the fence, if we are past the start time
     * use the end time to calculate fence
     *
     * @param startTime the startTime in LocalDateTime format
     * @param endTime   the endTime in LocalDateTime format
     * @return the relevant time if passed startTime will return endTime
     */
    public static long determineRelevantTime(LocalDateTime startTime, LocalDateTime endTime) {
        long currentTime = System.currentTimeMillis();
        long st = Converters.unixFromDateTime(startTime);
        long et = Converters.unixFromDateTime(endTime);
        return currentTime > st ? et : st;
    }

    public static boolean eventIsPassedCurrentTime(LocalDateTime eventTime){
        LocalDateTime now = Converters.dateTimeFromUnix(System.currentTimeMillis());
        return eventTime.isBefore(now);
    }
}
