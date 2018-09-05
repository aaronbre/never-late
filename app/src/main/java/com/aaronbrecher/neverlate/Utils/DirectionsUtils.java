package com.aaronbrecher.neverlate.Utils;

import android.location.Location;

import com.aaronbrecher.neverlate.models.Event;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;

import org.joda.time.Instant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
//TODO this class will be used to get direction information time etc.
//for now it uses the google API's may change that to a Mapbox or Mapquest
public class DirectionsUtils {

    /**
     * add the distance and duration to the Event using the Distance Matrix API
     * @param apiContext The GeoApiContext
     * @param events the list of events to get information about
     * @param location the users current location
     */
    public static void addDistanceInfoToEventList(GeoApiContext apiContext, List<Event> events, Location location){
        DistanceMatrixApiRequest dmRequest = DirectionsUtils.getDistanceMatrixApiRequest(apiContext, events, location);
        DistanceMatrix distanceMatrix =  null;
        try {
            distanceMatrix = dmRequest.await();
        } catch (InterruptedException | IOException | ApiException e) {
            e.printStackTrace();
        }
        if (distanceMatrix != null) {
            DistanceMatrixElement[] elements = distanceMatrix.rows[0].elements;
            for (int i = 0, j = elements.length; i < j; i++){
                DistanceMatrixElement element = elements[i];
                Event event = events.get(i);
                event.setDistance(element.distance.inMeters);
                //if there is a relative traffic time rather use that
                long timeTo = element.durationInTraffic != null ? element.durationInTraffic.inSeconds : element.duration.inSeconds;
                event.setTimeTo(timeTo);
            }
        }
    }


    private static DistanceMatrixApiRequest getDistanceMatrixApiRequest(GeoApiContext apiContext, List<Event> events, Location location){
        DistanceMatrixApiRequest req = new DistanceMatrixApiRequest(apiContext);

        ArrayList<String> dest = new ArrayList<>();
        for(Event event : events){
            dest.add(event.getLocation());
        }
        String [] destinationList = dest.toArray(new String[dest.size()]);
        return req.origins(new LatLng(location.getLatitude(), location.getLongitude()))
                .mode(TravelMode.DRIVING)
                .destinations(destinationList)
                .departureTime(Instant.now());
    }

    public static String readableTravelTime(long travelTime){
        int totalMinutes = (int) (travelTime/60);
        int hours = totalMinutes/60;
        int minutes = totalMinutes%60;
        return hours + ":" + minutes;
    }
}
