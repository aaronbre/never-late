package com.aaronbrecher.neverlate.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.models.Event;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;

import org.joda.time.Instant;
import org.joda.time.LocalDateTime;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//this class will be used to get direction information time etc.
//for now it uses the google API's may change that to a Mapbox or Mapquest
public class DirectionsUtils {

    /**
     * add the distance and duration to the Event using the Distance Matrix API
     *
     * @param apiContext The GeoApiContext
     * @param events     the list of events to get information about
     * @param location   the users current location
     */
    public static void addDistanceInfoToEventList(GeoApiContext apiContext, List<Event> events, Location location) {
        events = removeEventsWithoutLocation(events);
        DistanceMatrixApiRequest dmRequest = DirectionsUtils.getDistanceMatrixApiRequest(apiContext, events, location);
        if (dmRequest == null) return;
        DistanceMatrix distanceMatrix = null;
        try {
            distanceMatrix = dmRequest.await();
        } catch (InterruptedException | IOException | ApiException e) {
            e.printStackTrace();
        }
        if (distanceMatrix != null) {
            DistanceMatrixElement[] elements = distanceMatrix.rows[0].elements;
            for (int i = 0, j = elements.length; i < j; i++) {
                DistanceMatrixElement element = elements[i];
                Event event = events.get(i);
                event.setDistance(element.distance.inMeters);
                //if there is a relative traffic time rather use that
                long timeTo = element.durationInTraffic != null ? element.durationInTraffic.inSeconds : element.duration.inSeconds;
                event.setTimeTo(timeTo);
            }
        }
    }

    private static List<Event> removeEventsWithoutLocation(List<Event> eventList) {
        List<Event> filtered = new ArrayList<>();
        for (Event event : eventList) {
            if (event.getLocation() != null && !event.getLocation().equals("")) {
                filtered.add(event);
            }
        }
        return filtered;
    }


    private static DistanceMatrixApiRequest getDistanceMatrixApiRequest(GeoApiContext apiContext, List<Event> events, Location location) {
        DistanceMatrixApiRequest req = new DistanceMatrixApiRequest(apiContext);

        ArrayList<String> dest = new ArrayList<>();
        for (Event event : events) {
            dest.add(event.getLocation());
        }
        String[] destinationList = dest.toArray(new String[dest.size()]);
        if (destinationList.length == 0) return null;
        return req.origins(new LatLng(location.getLatitude(), location.getLongitude()))
                .mode(TravelMode.DRIVING)
                .destinations(destinationList)
                .departureTime(Instant.now());
    }

    public static String readableTravelTime(long travelTime) {
        int totalMinutes = (int) (travelTime / 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + ":" + minutes;
    }

    public static String getHumanReadableDistance(Context context, Long distance, SharedPreferences sharedPreferences){
        //TODO add a shared prefs to miles or km and fix this accordingly
        boolean useMetric = false;
        if(sharedPreferences.contains(Constants.UNIT_SYSTEM_PREFS_KEY)){
            useMetric = sharedPreferences.getBoolean(Constants.UNIT_SYSTEM_PREFS_KEY, false);
        }
        float km = distance.floatValue()/1000;
        DecimalFormat df = new DecimalFormat("#.#");
        if(useMetric){
            return df.format(km) + context.getString(R.string.km_signature);
        }else {
            double miles = LocationUtils.kmToMiles(km);
            return df.format(miles) + context.getString(R.string.miles_signature);
        }
    }

    public static String getTimeToLeaveHumanReadable(Context context, long timeTo, long eventTime){
        java.text.DateFormat dateFormat = DateFormat.getTimeFormat(context);

        timeTo = timeTo * 1000;
        long leaveTime = eventTime - timeTo;
        Date date = new Date(leaveTime);
        return DateUtils.getRelativeTimeSpanString(leaveTime, System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS).toString();
        //return dateFormat.format(date);
//        LocalDateTime localDateTime = new LocalDateTime(leaveTime);
//        String amPm;
//        int hour;
//        if(localDateTime.getHourOfDay() < 12){
//            hour = localDateTime.getHourOfDay();
//            amPm = context.getString(R.string.am);
//        } else {
//            hour = localDateTime.getHourOfDay() - 12;
//            amPm = context.getString(R.string.pm);
//        }
//        return hour + ":" + localDateTime.getMinuteOfHour() + " " + amPm;
    }
}
