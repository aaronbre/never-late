package com.aaronbrecher.neverlate.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.format.DateUtils;
import android.util.SparseArray;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventLocationDetails;
import com.aaronbrecher.neverlate.models.retrofitmodels.EventDistanceDuration;
import com.aaronbrecher.neverlate.network.AppApiService;
import com.aaronbrecher.neverlate.network.AppApiUtils;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Class that contains functions to get distance information,
 * will use the Here api https://developer.here.com/
 */
public class DirectionsUtils {

    private SharedPreferences mSharedPreferences;
    private Location mLocation;
    private double mspeed;

    public DirectionsUtils(SharedPreferences preferences, Location location) {
        mSharedPreferences = preferences;
        mLocation = location;
        mspeed = Double.valueOf(mSharedPreferences.getString(Constants.SPEED_PREFS_KEY, "0.666667"));

    }

    /**
     * function to add distance information (Distance,Duration) to events
     * TODO for subscriptions - will execute this code always, if server response is that the user is not subscribed than will use as the crow flies data instead
     *
     * @param events   list of events from the calendar
     */
    public boolean addDistanceInfoToEventList(List<Event> events) {
        if (mLocation == null) return false;
        events = removeEventsWithoutLocation(events);
        SparseArray<List<Event>> transitTypes = splitEventListByTrasportType(events);
        return executeDrivingQuery(transitTypes.get(Constants.TRANSPORT_DRIVING))
                && executeTransitQuery(transitTypes.get(Constants.TRANSPORT_PUBLIC));
    }

    //Filter out all events that do not have a valid location
    private List<Event> removeEventsWithoutLocation(List<Event> eventList) {
        List<Event> filtered = new ArrayList<>();
        for (Event event : eventList) {
            if (event.getLocationLatlng() != null) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    /**
     * This query will be to check for driving
     *
     * @return true if the data was added (even partially) false if not
     */
    private boolean executeDrivingQuery(List<Event> events) {
        return executeHereMatrixQuery(events, false);
    }

    /**
     * This query will be to check for public transportation data
     *
     * @return true if the data was added (even partially) false if not
     */
    private boolean executeTransitQuery(List<Event> events) {
        return executeHereMatrixQuery(events, true);
    }


    private boolean executeHereMatrixQuery(List<Event> events, boolean forPublicTransit) {
        if (events.size() == 0) return true;
        String origin = mLocation.getLatitude() + "," + mLocation.getLongitude();
        List<EventLocationDetails> destinations = convertEventListForQuery(events, forPublicTransit);
        AppApiService service = AppApiUtils.createService();

        Call<List<EventDistanceDuration>> request = forPublicTransit ? service.queryHerePublicTransit(origin, destinations)
                : service.queryHereMatrix(origin, destinations);
        try {
            Response<List<EventDistanceDuration>> response = request.execute();
            if (response.code() == 403) {
                addCrowFliesDistanceInfo(events);
                return true;
            }
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

    private void addCrowFliesDistanceInfo(List<Event> events) {
        for (Event event : events) {
            Location eventLocation = new Location("never-late");
            eventLocation.setLatitude(event.getLocationLatlng().latitude);
            eventLocation.setLongitude(event.getLocationLatlng().longitude);
            long distance = (long) mLocation.distanceTo(eventLocation);
            if(distance <= 0) continue;
            long distanceInKilometers = distance/1000;
            long time = (long) (distanceInKilometers/mspeed) * 60;
            event.setDrivingTime(time);
            event.setDistance(distance);
        }
    }

    /**
     * convert the event list to a more concise representation to send to server, will include the latitude,
     * longitude, as well as the arrival time
     *
     * @param events             list of all the events
     * @param forPublicTransport if for a public transit request need to add the arrival time in iso-format
     * @return a list of the minimized event objects
     */
    private List<EventLocationDetails> convertEventListForQuery(List<Event> events, boolean forPublicTransport) {
        List<EventLocationDetails> destinations = new ArrayList<>();
        for (Event event : events) {
            long eventTime = GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime());
            EventLocationDetails locationDetails = new EventLocationDetails(String.valueOf(event.getLocationLatlng().latitude),
                    String.valueOf(event.getLocationLatlng().longitude));
            if (forPublicTransport) {
                //TODO create iso-time for event location
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
                String formatted = dateFormat.format(new Date(eventTime));
                locationDetails.setArrivalTime(formatted);
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
    private SparseArray<List<Event>> splitEventListByTrasportType(List<Event> eventList) {
        List<Event> drivingEvents = new ArrayList<>();
        List<Event> publicEvents = new ArrayList<>();
        SparseArray<List<Event>> splitMap = new SparseArray<>();
        for (Event event : eventList) {
            switch (event.getTransportMode()) {
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
            return df.format(km) + " " + context.getString(R.string.km_signature);
        } else {
            double miles = LocationUtils.kmToMiles(km);
            return df.format(miles) + " " + context.getString(R.string.miles_signature);
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
