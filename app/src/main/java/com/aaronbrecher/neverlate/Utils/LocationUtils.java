package com.aaronbrecher.neverlate.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
import android.util.Pair;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class LocationUtils {
    public static FusedLocationProviderClient sClient =  null;

    public static LatLng latlngFromAddress(Context context, String address){
        Geocoder geocoder = new Geocoder(context);
        List<Address> addresses;
        LatLng latLng = null;
        try{
            addresses = geocoder.getFromLocationName(address, 1);
            if(address == null) return null;
            Address location = addresses.get(0);
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

        }catch (IOException e){
            e.printStackTrace();
        }
        return latLng;
    }

    @SuppressLint("MissingPermission")
    public static void getDeviceLocation(Context context) {
        if(sClient == null) sClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Method to calculate the distance from users location to the current donation center
     * @param userLocation Location object with lat/lon of the user
     * @param destination - location of the destination
     * @return the distance between the two locations as a string formatted to one decimal point
     */
    public static String getDistance(Location userLocation, Location destination) {
        float distance = userLocation.distanceTo(destination)/1000;
        boolean useMiles = true;
        if(isUsa()){
            double miles = kmToMiles(distance);
            DecimalFormat df = new DecimalFormat("#");
            return df.format(miles) + "\nMILES";
        }else{
            DecimalFormat df = new DecimalFormat("#");
            return df.format(distance) + "\nKM";
        }
    }

    private static boolean isUsa() {
        //TODO check if the user is in the US if so use miles can use TelephonyManager but need context...
        return false;
    }

    private static double kmToMiles(float kilometers){
        return kilometers * .621;
    }

}
