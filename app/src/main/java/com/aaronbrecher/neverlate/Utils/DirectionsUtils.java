package com.aaronbrecher.neverlate.Utils;

import android.location.Location;

import com.aaronbrecher.neverlate.models.Event;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;

import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.List;
//TODO this class will be used to get direction information time etc.
//for now it uses the google API's may change that to a Mapbox or Mapquest
public class DirectionsUtils {


    public static DistanceMatrixApiRequest getDistanceMatrixApiRequest(GeoApiContext apiContext, List<Event> events, Location location){
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
}
