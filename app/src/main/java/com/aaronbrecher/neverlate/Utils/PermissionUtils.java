package com.aaronbrecher.neverlate.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.aaronbrecher.neverlate.R;

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;


public class PermissionUtils {

    public static final String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALENDAR};

    //check to make sure all the permissions were granted
    public static boolean hasPermissions(Context context) {
        for(String permission : permissions){
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * Request permissions for the location and calendar, show a snackbar if the permissions were denied
     * previously
     * @param activity the activity which is requesting the permissions
     * @param view the rootView to use to base the snackbar
     */
    public static void requestCalendarAndLocationPermissions(final Activity activity, View view) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALENDAR)) {
            Snackbar.make(view, R.string.permissions_explanation_text,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(activity, permissions,
                                    PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();

        } else {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_REQUEST_CODE);
        }
    }

    //given an array of permission results confirm that they were granted
    public static boolean verifyPermissions(int[] grantResults){
        if(grantResults.length < 1){
            return false;
        }
        for (int result: grantResults){
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }
}
