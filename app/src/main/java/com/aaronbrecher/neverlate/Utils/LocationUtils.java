package com.aaronbrecher.neverlate.Utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

public class LocationUtils {

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
}
