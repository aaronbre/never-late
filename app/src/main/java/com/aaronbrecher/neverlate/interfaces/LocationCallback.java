package com.aaronbrecher.neverlate.interfaces;

import android.location.Location;

public interface LocationCallback {
    void successCallback(Location location);
    void failedCallback();
}
