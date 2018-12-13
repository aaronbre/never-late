package com.aaronbrecher.neverlate.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.format.DateUtils;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventLocationDetails;
import com.aaronbrecher.neverlate.models.retrofitmodels.DirectionsDuration;
import com.aaronbrecher.neverlate.models.retrofitmodels.EventDistanceDuration;
import com.aaronbrecher.neverlate.models.retrofitmodels.MapboxDirectionMatrix.MapboxDirectionMatrix;
import com.aaronbrecher.neverlate.models.retrofitmodels.googleDistanceMatrix.DistanceMatrix;
import com.aaronbrecher.neverlate.models.retrofitmodels.googleDistanceMatrix.Element;
import com.aaronbrecher.neverlate.network.AppApiUtils;
import com.aaronbrecher.neverlate.network.AppApiService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Class that contains functions to get distance information,
 * will use the Here api https://developer.here.com/
 */
public class DirectionsUtils {
    //Max allowable destinations for DistanceMatrix request
    private static final int MAX_QUERY_SIZE = 99;

    /**
     * function to add distance information (Distance,Duration) to events. The query will be
     * constrained by request limits (for Google it is 24) so if the list is more than that will
     * create sublists to do multiple requests
     *
     * @param events   list of events from the calendar
     * @param location the users current location
     */
    public static boolean addDistanceInfoToEventList(List<Event> events, Location location) {
        if (location == null) return false;
        boolean wasAdded = false;
        events = removeEventsWithoutLocation(events);
        if (events.size() > MAX_QUERY_SIZE) {
            List<List<Event>> lists = splitList(events);
            for (List<Event> list : lists) {
                wasAdded = wasAdded || executeHereMatrixQuery(list, location);
            }
            return wasAdded;
        } else {
            return executeHereMatrixQuery(events, location);
        }
    }

    /**
     * function to chunk original list
     * @param events the original list provided by the calendar
     * @return a list of lists each with a size less then @MAX_QUERY_SIZE
     */
    private static List<List<Event>> splitList(List<Event> events) {
        List<List<Event>> lists = new ArrayList<>();
        for (int i = 0; i < events.size(); i += MAX_QUERY_SIZE) {
            lists.add(events.subList(i, Math.min(i + MAX_QUERY_SIZE, events.size())));
        }
        return lists;
    }

    //Filter out all events that do not have a valid location
    private static List<Event> removeEventsWithoutLocation(List<Event> eventList) {
        List<Event> filtered = new ArrayList<>();
        for (Event event : eventList) {
            if (event.getLocationLatlng() != null) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private static boolean executeHereMatrixQuery(List<Event> events, Location location) {
        String origin = location.getLatitude() + "," + location.getLongitude();
        List<EventLocationDetails> destinations = convertEventListForQuery(events, false);
        AppApiService service = AppApiUtils.createService();
        Call<List<EventDistanceDuration>> request = service.queryHereMatrix(origin, destinations);
        try {
            Response<List<EventDistanceDuration>> response = request.execute();
            List<EventDistanceDuration> durationList = response.body();
            if (durationList == null || durationList.size() < 1) return false;
            for (int i = 0; i < durationList.size(); i++) {
                Event event = events.get(i);
                EventDistanceDuration distanceDuration = durationList.get(i);
                event.setDistance((long) distanceDuration.getDistance());
                event.setDrivingTime((long) distanceDuration.getDuration());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * This query will be to check for public transportation data
     *
     * @return true if the data was added (even partially) false if not
     */
    private static boolean executeTransitQuery(List<Event> events, Location location) {
        List<EventLocationDetails> destinations = convertEventListForQuery(events, true);
        String origin = location.getLatitude() + "," + location.getLongitude();
        AppApiService service = AppApiUtils.createService();
        Call<List<EventDistanceDuration>> request = service.queryHereMatrix(origin, destinations);
        try {
            Response<List<EventDistanceDuration>> response = request.execute();
            List<EventDistanceDuration> distanceMatrix = response.body();
            if (distanceMatrix == null || distanceMatrix.size() < 1)
                return false;
            for (int i = 0; i < distanceMatrix.size(); i++) {
                EventDistanceDuration distanceDuration = distanceMatrix.get(i);
                Event event = events.get(i);
                event.setDrivingTime((long) distanceDuration.getDuration());
                event.setDistance((long) distanceDuration.getDistance());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * convert the event list to a more concise representation to send to server, will include the latitude,
     * longitude, as well as the arrival time
     * @param events list of all the events
     * @param forPublicTransport if for a public transit request need to add the arrival time in iso-format
     * @return a list of the minimized event objects
     */
    private static List<EventLocationDetails> convertEventListForQuery(List<Event> events, boolean forPublicTransport) {
        List<EventLocationDetails> destinations = new ArrayList<>();
        for (Event event : events) {
            long eventTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
            EventLocationDetails locationDetails = new EventLocationDetails(String.valueOf(event.getLocationLatlng().latitude),
                    String.valueOf(event.getLocationLatlng().longitude));
            if (forPublicTransport) {
                //TODO create iso-time for event location
                // locationDetails.setArrivalTime();
            }
            destinations.add(locationDetails);
        }
        return destinations;
    }

    /**
     * Function to split the events into different driving catagories
     *
     * @return a map containing lists corresponding to all driving types
     */
    private static Map<Integer, List<Event>> splitEventListByTrasportType(List<Event> eventList) {
        List<Event> drivingEvents = new ArrayList<>();
        List<Event> walkingEvents = new ArrayList<>();
        List<Event> publicEvents = new ArrayList<>();
        HashMap<Integer, List<Event>> splitMap = new HashMap<>();
        for (Event event : eventList) {
            switch (event.getTransportMode()) {
                case Constants.TRANSPORT_WALKING:
                    walkingEvents.add(event);
                    break;
                case Constants.TRANSPORT_PUBLIC:
                    publicEvents.add(event);
                    break;
                case Constants.TRANSPORT_DRIVING:
                default:
                    drivingEvents.add(event);
                    break;
            }
        }
        splitMap.put(Constants.TRANSPORT_DRIVING, drivingEvents);
        splitMap.put(Constants.TRANSPORT_WALKING, walkingEvents);
        splitMap.put(Constants.TRANSPORT_PUBLIC, publicEvents);
        return splitMap;
    }



    //TODO move these functions to a different class
    /**
     * Returns a readable string of distance to the event either in
     * Miles or KM
     */
    public static String getHumanReadableDistance(Context context, Long distance, SharedPreferences sharedPreferences) {
        //TODO add a shared prefs to miles or km and fix this accordingly
        boolean useMetric = true;

        String unitType = sharedPreferences.getString(context.getString(R.string.pref_units_key), "");
        if (context.getString(R.string.pref_units_imperial).equals(unitType)) {
            useMetric = false;
        }
        float km = distance.floatValue() / 1000;
        DecimalFormat df = new DecimalFormat("#.#");
        if (useMetric) {
            return df.format(km) + context.getString(R.string.km_signature);
        } else {
            double miles = LocationUtils.kmToMiles(km);
            return df.format(miles) + context.getString(R.string.miles_signature);
        }
    }

    /**
     * Returns a human readable representation of the time to leave to the
     * event
     *
     * @param timeTo    time until the event in seconds
     * @param eventTime time of the event in millis
     * @return Text of how much time to leave
     */
    public static String getTimeToLeaveHumanReadable(long timeTo, long eventTime) {
        timeTo = timeTo * 1000;
        long leaveTime = eventTime - timeTo;
        return DateUtils.getRelativeTimeSpanString(leaveTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();

    }


    public static String readableTravelTime(long travelTime) {
        int totalMinutes = (int) (travelTime / 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + ":" + minutes;
    }
}
