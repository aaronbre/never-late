package com.aaronbrecher.neverlate.interfaces;

import android.location.Location;

public interface LocationCallback {
    void getLocationSuccessCallback(Location location);
    void getLocationFailedCallback();
}
