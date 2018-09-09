package com.aaronbrecher.neverlate.Utils;

import android.util.Log;

import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.models.Event;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;

import java.util.List;

public class GeofenceUtils {
    private static final String TAG = GeofenceUtils.class.getSimpleName();

    /**
     * Get the fence Radius based on the time and the kmPerMinute Multiplier
     * Radius will be determined by roundtrip to point (hence divided by 2)
     *
     * @param timeToEvent the time to the event either will be start or end
     * @param kmPerMinute as it sounds - when DistanceMatrix was not able to be used this
     *                    will be a constant value from shared prefs, otherwise this will
     *                    be determined by getting the average speed from all events
     * @return an integer value of the rounded radius in meters
     */
    public static int getFenceRadius(long timeToEvent, double kmPerMinute) {
        long millisToEvent = (timeToEvent - System.currentTimeMillis());
        long minutesToEvent = millisToEvent / 60000;
        double kmRadius = (minutesToEvent * kmPerMinute);
        double meterRadius = kmRadius * 1000;
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

    public static boolean eventIsPassedCurrentTime(LocalDateTime eventTime){
        LocalDateTime now = Converters.dateTimeFromUnix(System.currentTimeMillis());
        return now.isBefore(eventTime);
    }

    public static int getFenceRadius(long distance, long drivingTime, long eventTime) {
        long minutesToevent = (eventTime - System.currentTimeMillis()) / 60000;
        int drivingMinutes = (int) drivingTime / 60;
        long kmDistance = distance / 1000;
        float kmPerMinute = (float) kmDistance / drivingMinutes;
        int radius = (int) Math.round(kmPerMinute * minutesToevent) * 1000;
        return radius > 100 ? radius : 100;
    }

    public static double getAverageSpeed(List<Event> events) {
        int numEvents = 0;
        double speedSum = 0;
        for (Event event : events) {
            if (event.getDistance() != null && event.getTimeTo() != null) {
                numEvents++;
                speedSum += getSpeed(event.getDistance(), event.getTimeTo());
            }
        }
        return numEvents == 0 ? 0 : speedSum / numEvents;
    }

    public static double getSpeed(long distance, long drivingTime) {
        int drivingMinutes = (int) drivingTime / 60;
        long kmDistance = distance / 1000;
        return (double) kmDistance / drivingMinutes;
    }
}
