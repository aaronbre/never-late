package com.aaronbrecher.neverlate.database;


import androidx.room.TypeConverter;

import com.aaronbrecher.neverlate.models.EventCompatibility.Compatible;
import com.google.android.gms.maps.model.LatLng;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

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
    public static LatLng latlngFromString(String string) {
        if (string == null) return null;
        String[] strings = string.split(",");
        double lat = Double.parseDouble(strings[0]);
        double lon = Double.parseDouble(strings[1]);
        return new LatLng(lat, lon);
    }

    @TypeConverter
    public static String stringFromLatLng(LatLng latLng) {
        if (latLng == null) return null;
        return latLng.latitude + "," + latLng.longitude;
    }

    @TypeConverter
    public static Compatible compatibleFromInteger(int compat) {
        switch (compat) {
            case 0:
                return Compatible.FALSE;
            case 1:
                return Compatible.TRUE;
            default:
                return Compatible.UNKNOWN;
        }
    }

    @TypeConverter
    public static int intFromCompatible(Compatible compatible) {
        switch (compatible) {
            case TRUE:
                return 1;
            case FALSE:
                return 0;
            default:
                return 2;
        }
    }
}
