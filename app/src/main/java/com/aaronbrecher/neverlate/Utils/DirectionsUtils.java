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

//this class will be used to get direction information time etc.
//for now it uses the google API's may change that to a Mapbox or Mapquest
public class DirectionsUtils {

    private static final String NOT_FOUND = "NOT_FOUND";
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
     *
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

    private static boolean executeHereMatrixQuery(List<Event> events, Location location){
        String origin = location.getLatitude() + "," + location.getLongitude();
        List<EventLocationDetails> destinations = convertEventListForQuery(events, false);
        AppApiService service = AppApiUtils.createService();
        Call<List<EventDistanceDuration>> request = service.queryHereMatrix(origin, destinations);
        try{
            Response<List<EventDistanceDuration>> response = request.execute();
            List<EventDistanceDuration> durationList = response.body();
            if(durationList == null || durationList.size() < 1) return false;
            for(int i = 0; i < durationList.size();i++){
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

    private static List<EventLocationDetails> convertEventListForQuery(List<Event> events, boolean forPublicTransport) {
        List<EventLocationDetails> destinations = new ArrayList<>();
        for(Event event : events){
            long eventTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
            EventLocationDetails locationDetails = new EventLocationDetails(String.valueOf(event.getLocationLatlng().latitude),
                    String.valueOf(event.getLocationLatlng().longitude));
            if(forPublicTransport){
                //TODO create iso-time for event location
                // locationDetails.setArrivalTime();
            }
            destinations.add(locationDetails);
        }
        return destinations;
    }


    //TODO add an additional parameter here and in retrofit to query walking as well
    private static boolean executeMapboxQuery(List<Event> events, Location location) {
        String destinations = getDestinationsLngLatAsString(events);
        String origin = location.getLongitude() + "," + location.getLatitude();
        AppApiService service = AppApiUtils.createService();
        Call<MapboxDirectionMatrix> request = service.queryMapboxDirectionMatrix(origin, destinations, events.size());
        try {
            Response<MapboxDirectionMatrix> response = request.execute();
            MapboxDirectionMatrix directionMatrix = response.body();
            //Sanity check to make sure everything is good
            if (directionMatrix == null || directionMatrix.getDurations() == null || directionMatrix.getDurations().size() < 1)
                return false;

            //get the list of distances and durations these will always be the same size
            List<Double> durations = directionMatrix.getDurations().get(0);
            List<Double> distances = directionMatrix.getDistances().get(0);
            if (durations.size() < 1 || distances.size() < 1) return false;
            for (int i = 0, j = durations.size(); i < j; i++) {
                Double distance = distances.get(i);
                Double duration = durations.get(i);
                //if this particular route failed skip it
                if (distance == null || duration == null) continue;
                Event event = events.get(i);
                event.setDistance(distance.longValue());
                //if there is a relative traffic time rather use that
                event.setDrivingTime(duration.longValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static List<Event> removeEventsWithoutLocation(List<Event> eventList) {
        List<Event> filtered = new ArrayList<>();
        for (Event event : eventList) {
            if (event.getLocationLatlng() != null) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * Function to split the events into different driving catagories
     * @return a map containing lists corresponding to all driving types
     */
    private static Map<Integer, List<Event>> splitEventListByTrasportType(List<Event> eventList){
        List<Event> drivingEvents = new ArrayList<>();
        List<Event> walkingEvents = new ArrayList<>();
        List<Event> publicEvents = new ArrayList<>();
        HashMap<Integer, List<Event>> splitMap = new HashMap<>();
        for(Event event : eventList){
            switch (event.getTransportMode()){
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

    /**
     * Converts list of events to a comma seperated list of lat and long
     * each lat and long will seperated by a space ex. 35.857399 24.34114
     * this format will be needed for using mapbox
     */
    private static String getDestinationsLngLatAsString(List<Event> events){
        ArrayList<String> dest = new ArrayList<>();
        for(Event event : events){
            if(event.getLocationLatlng() == null) continue;
            String lngLatString = event.getLocationLatlng().longitude + "," + event.getLocationLatlng().latitude;
            dest.add(lngLatString);
        }
        return android.text.TextUtils.join(";", dest);
    }

    /**
     * This query will be to the google API as mapbox does not support public transit
     * @return true if the data was added (even partially) false if not
     */
    private static boolean executeTransitQuery(List<Event> events, Location location, int numTries) {
        String destinations = getDestinationsAsString(events);
        String origin = location.getLatitude() + "," + location.getLongitude();
        AppApiService service = AppApiUtils.createService();
        Call<DistanceMatrix> request = service.queryDistanceMatrix(origin, destinations);
        try {
            Response<DistanceMatrix> response = request.execute();
            DistanceMatrix distanceMatrix = response.body();
            if (distanceMatrix == null || distanceMatrix.getRows() == null || distanceMatrix.getRows().get(0) == null)
                return false;
            List<Element> elements = distanceMatrix.getRows().get(0).getElements();
            if (elements.size() < 1) return false;
            for (int i = 0, j = elements.size(); i < j; i++) {
                Element element = elements.get(i);
                if (element.getStatus().equals(NOT_FOUND)) continue;
                Event event = events.get(i);
                event.setDistance(element.getDistance().getValue().longValue());
                //if there is a relative traffic time rather use that
                long timeTo = element.getDurationInTraffic() != null ? element.getDurationInTraffic().getValue() : element.getDuration().getValue();
                event.setDrivingTime(timeTo);
            }
        } catch (IOException e) {
            e.printStackTrace();
                return false;
        }
        return true;
    }

    /**
     * Converts a list of events to a comma seperated list of the destinations
     * this is needed in order to query the custom API using retrofit
     */
    private static String getDestinationsAsString(List<Event> events) {
        ArrayList<String> dest = new ArrayList<>();
        for (Event event : events) {
            String location = event.getLocation();
            location = location.replaceAll(",", " ");
            dest.add(location);
        }
        return android.text.TextUtils.join(",", dest);
    }


    /**
     * Returns a readable string of distance to the event either in
     * Miles or KM
     */
    public static String getHumanReadableDistance(Context context, Long distance, SharedPreferences sharedPreferences) {
        //TODO add a shared prefs to miles or km and fix this accordingly
        boolean useMetric = true;

        String unitType = sharedPreferences.getString(context.getString(R.string.pref_units_key), "");
        if(context.getString(R.string.pref_units_imperial).equals(unitType)){
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
