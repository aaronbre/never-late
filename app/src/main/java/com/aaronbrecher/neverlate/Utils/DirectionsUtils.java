package com.aaronbrecher.neverlate.Utils;

import com.aaronbrecher.neverlate.BuildConfig;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
//TODO this class will be used to get direction information time etc.
//for now it uses the google API's may change that to a Mapbox or Mapquest
public class DirectionsUtils {

    public static DirectionsApiRequest getDirectionsApiRequest(com.google.android.gms.maps.model.LatLng origin,
                                                               com.google.android.gms.maps.model.LatLng destination) {
        GeoApiContext geoApiContext = new GeoApiContext();
        geoApiContext.setQueryRateLimit(3)
                .setApiKey(BuildConfig.DIRECTIONS_API_KEY)
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);

        DateTime now = new DateTime();
        return DirectionsApi.newRequest(geoApiContext)
                .mode(TravelMode.DRIVING)
                .origin(convertLatLng(origin))
                .destination(convertLatLng(destination))
                .departureTime(now);
    }

    private static LatLng convertLatLng(com.google.android.gms.maps.model.LatLng latLng){
        return new LatLng(latLng.latitude, latLng.longitude);
    }
}
