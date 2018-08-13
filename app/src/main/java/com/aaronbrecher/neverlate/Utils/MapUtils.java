package com.aaronbrecher.neverlate.Utils;

import android.util.Log;

import com.aaronbrecher.neverlate.database.Converters;

import org.threeten.bp.LocalDateTime;

public class MapUtils {
    private static final String TAG = MapUtils.class.getSimpleName();

    /**
     * Get the fence Radius based on the time and the milesPerMinute Multiplier
     * Radius will be determined by roundtrip to point (hence divided by 2)
     * @param timeToEvent the time to the event either will be start or end
     * @return an integer value of the rounded radius in meters
     */
    public static int getFenceRadius(long timeToEvent, double milesPerMinute) {
        long millisToEvent = (timeToEvent - System.currentTimeMillis());
        long minutesToEvent = millisToEvent / 60000;
        double milesRadius = (minutesToEvent * milesPerMinute)/2;
        double meterRadius = (milesRadius * 1.609) * 1000;
        Log.i(TAG, "getFenceRadius: " + Math.round(meterRadius));
        int radius = (int) Math.round(meterRadius);
        return radius > 100 ? radius : 100;
    }

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
}
