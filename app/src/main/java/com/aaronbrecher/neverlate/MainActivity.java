package com.aaronbrecher.neverlate;

import android.Manifest;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.aaronbrecher.neverlate.Utils.PermissionUtils;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_CODE = 101;
    private View mlayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mlayout = findViewById(R.id.main_container);

        if (!PermissionUtils.hasPermissions(this)) {
            PermissionUtils.requestCalendarAndLocationPermissions(this);
        }
    }

//    private void requestCalendarAndLocationPermissions() {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
//                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) ||
//                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)) {
//            Snackbar.make(mlayout, R.string.permissions_explanation_text,
//                    Snackbar.LENGTH_INDEFINITE)
//                    .setAction(R.string.ok, new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            ActivityCompat.requestPermissions(MainActivity.this, PermissionUtils.permissions,
//                                    PERMISSIONS_REQUEST_CODE);
//                        }
//                    })
//                    .show();
//
//        } else {
//            ActivityCompat.requestPermissions(this, PermissionUtils.permissions, PERMISSIONS_REQUEST_CODE);
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PermissionUtils.verifyPermissions(grantResults)) {
                //do app initialization code...
            } else {
                PermissionUtils.requestCalendarAndLocationPermissions(this);
                //Show image showing error with button to rerequest permissions...
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
