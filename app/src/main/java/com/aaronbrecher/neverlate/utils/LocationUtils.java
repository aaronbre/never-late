package com.aaronbrecher.neverlate.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

public class LocationUtils {

    /**
     * Use the Geocoder API to convert given address to it's LatLng location
     */
    public static LatLng latlngFromAddress(Context context, String address) {
        Geocoder geocoder = new Geocoder(context);
        List<Address> addresses;
        LatLng latLng = null;
        try {
            addresses = geocoder.getFromLocationName(address, 1);
            if (addresses == null || addresses.size() == 0) return null;
            Address location = addresses.get(0);
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return latLng;
    }

    static double kmToMiles(float kilometers) {
        return kilometers * .621;
    }

    //Convert a Location object to a string representation
    public static String locationToLatLngString(Location location) {
        if(location == null) return "";
        return location.getLatitude() + "," + location.getLongitude();
    }

    /**
     * Convert a String representation of Latitude and Longitude to a
     * Location Object, Used to store the location in SharedPrefs after issues wiht Parceable
     *
     * @param latLng String of Lat Long
     * @return Location using provided Lat and Long
     */
    public static Location locationFromLatLngString(String latLng) {
        if (latLng.isEmpty()) return null;
        String[] l = latLng.split(",");
        double latitude;
        double longitude;
        try {
            latitude = Double.valueOf(l[0]);
            longitude = Double.valueOf(l[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        Location location = new Location("neverlate");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }
}
