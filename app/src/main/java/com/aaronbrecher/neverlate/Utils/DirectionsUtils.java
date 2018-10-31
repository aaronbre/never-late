package com.aaronbrecher.neverlate.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.format.DateUtils;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.retrofitmodels.DistanceMatrix;
import com.aaronbrecher.neverlate.models.retrofitmodels.Element;
import com.aaronbrecher.neverlate.network.AppApiUtils;
import com.aaronbrecher.neverlate.network.AppApiService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

//this class will be used to get direction information time etc.
//for now it uses the google API's may change that to a Mapbox or Mapquest
public class DirectionsUtils {

    private static final String NOT_FOUND = "NOT_FOUND";
    //Max allowable destinations for DistanceMatrix request
    private static final int MAX_QUERY_SIZE = 24;

    /**
     * function to add distance information (Distance,Duration) to events. The query will be
     * constrained by request limits (for Google it is 24) so if the list is more than that will
     * create sublists to do multiple requests
     * @param events list of events from the calendar
     * @param location the users current location
     */
    public static boolean addDistanceInfoToEventList(List<Event> events, Location location){
        if (location == null) return false;
        boolean wasAdded = false;
        events = removeEventsWithoutLocation(events);
        if(events.size() > MAX_QUERY_SIZE){
            List<List<Event>> lists = splitList(events);
            for(List<Event> list : lists){
               wasAdded = wasAdded || executeQuery(list, location, 0);
            }
            return wasAdded;
        }else {
            return executeQuery(events, location, 0);
        }
    }

    /**
     * function to chunk original list
     * @param events the original list provided by the calendar
     * @return a list of lists each with a size less then @MAX_QUERY_SIZE
     */
    private static List<List<Event>> splitList(List<Event> events) {
        List<List<Event>> lists = new ArrayList<>();
        for(int i = 0; i < events.size(); i += MAX_QUERY_SIZE){
            lists.add(events.subList(i, Math.min(i+MAX_QUERY_SIZE, events.size())));
        }
        return lists;
    }

    private static boolean executeQuery(List<Event> events, Location location, int numTries) {
        String destinations = getDestinationsAsString(events);
        String origin = location.getLatitude() + "," + location.getLongitude();
        AppApiService service = AppApiUtils.createService();
        Call<DistanceMatrix> request = service.queryDistanceMatrix(origin, destinations);
        try{
            Response<DistanceMatrix> response = request.execute();
            DistanceMatrix distanceMatrix = response.body();
            if(distanceMatrix == null || distanceMatrix.getRows() == null || distanceMatrix.getRows().get(0) == null) return false;
            List<Element> elements = distanceMatrix.getRows().get(0).getElements();
            if(elements.size() < 1) return false;
            for (int i = 0, j = elements.size(); i < j; i++) {
                Element element = elements.get(i);
                if(element.getStatus().equals(NOT_FOUND)) continue;
                Event event = events.get(i);
                event.setDistance(element.getDistance().getValue());
                //if there is a relative traffic time rather use that
                long timeTo = element.getDurationInTraffic() != null ? element.getDurationInTraffic().getValue() : element.getDuration().getValue();
                event.setTimeTo(timeTo);
            }
        }catch (IOException e){
            e.printStackTrace();
            if(e instanceof SocketTimeoutException && numTries < 2){
                //TODO if there is a recursion problem it is from here!!!!
                return executeQuery(events, location, numTries + 1);
            } else {
                return false;
            }
        }
        return true;
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

    /**
     * Converts a list of events to a comma seperated list of the destinations
     * this is needed in order to query the custom API using retrofit
     */
    private static String getDestinationsAsString(List<Event> events){
        ArrayList<String> dest = new ArrayList<>();
        for (Event event : events) {
            String location = event.getLocation();
            location = location.replaceAll(","," ");
            dest.add(location);
        }
        return android.text.TextUtils.join(",", dest);
    }

    public static String readableTravelTime(long travelTime) {
        int totalMinutes = (int) (travelTime / 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + ":" + minutes;
    }

    /**
     * Returns a readable string of distance to the event either in
     * Miles or KM
     */
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

    /**
     * Returns a human readable representation of the time to leave to the
     * event
     * @param timeTo time until the event in seconds
     * @param eventTime time of the event in millis
     * @return Text of how much time to leave
     */
    public static String getTimeToLeaveHumanReadable(long timeTo, long eventTime){
        timeTo = timeTo * 1000;
        long leaveTime = eventTime - timeTo;
        return DateUtils.getRelativeTimeSpanString(leaveTime, System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS).toString();
        //Date date = new Date(leaveTime);
        //java.text.DateFormat dateFormat = DateFormat.getTimeFormat(context);
        //return dateFormat.format(date);
    }
}
