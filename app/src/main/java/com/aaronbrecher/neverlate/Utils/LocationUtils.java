package com.aaronbrecher.neverlate.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import com.aaronbrecher.neverlate.Constants;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class LocationUtils {
    /**
     * this code needs an internet connection and takes up time...
     * TODO possibly change DB schema to hold a latlng and do this in the initial load to DB
     * @param context
     * @param address
     * @return
     */
    public static LatLng latlngFromAddress(Context context, String address) {
        Geocoder geocoder = new Geocoder(context);
        List<Address> addresses;
        LatLng latLng = null;
        try {
            addresses = geocoder.getFromLocationName(address, 1);
            if (addresses == null) return null ;
            Address location = addresses.get(0);
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return latLng;
    }

    public static double kmToMiles(float kilometers) {
        return kilometers * .621;
    }

    public static String locationToLatLngString(Location location){
        return location.getLatitude() + "," + location.getLongitude();
    }

    public static Location locationFromLatLngString(String latLng){
        String[] l = latLng.split(",");
        double latitude = Double.valueOf(l[0]);
        double longitude = Double.valueOf(l[1]);
        Location location = new Location("neverlate");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }
}
