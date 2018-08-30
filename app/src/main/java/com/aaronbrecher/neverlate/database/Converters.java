package com.aaronbrecher.neverlate.database;


import android.arch.persistence.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.TimeZone;

public class Converters {
    @TypeConverter
    public static LocalDateTime dateTimeFromUnix(Long value) {
        return value == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }

    @TypeConverter
    public static Long unixFromDateTime(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @TypeConverter
    public static LatLng latlngFromString(String string){
        String[] strings = string.split(",");
        double lat = Double.parseDouble(strings[0]);
        double lon = Double.parseDouble(strings[1]);
        return new LatLng(lat, lon);
    }

    @TypeConverter
    public static String stringFromLatLng(LatLng latLng){
        if(latLng == null) return null;
        return latLng.latitude +  "," + latLng.longitude;
    }
}
