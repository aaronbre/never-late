package com.aaronbrecher.neverlate.ui.controllers;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.models.retrofitmodels.Version;
import com.aaronbrecher.neverlate.network.AppApiService;
import com.aaronbrecher.neverlate.network.AppApiUtils;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.aaronbrecher.neverlate.ui.activities.NeedUpdateActivity;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.kobakei.ratethisapp.RateThisApp;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivityController {
    private MainActivity mActivity;
    private FirebaseJobDispatcher mFirebaseJobDispatcher;
    private NavController mNavController;

    public MainActivityController(MainActivity activity, NavController navController) {
        mActivity = activity;
        mFirebaseJobDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(mActivity));
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(mActivity);
        mNavController = navController;
    }

    public void setUpActivityMonitoring() {
        mFirebaseJobDispatcher.mustSchedule(BackgroundUtils.setUpActivityRecognitionJob(mFirebaseJobDispatcher));
    }

    //This will only set the alarm if it wasn't already set there will be a
    //seperate broadcast reciever to schedule alarm after boot...
    public void createRecurringCalendarCheck() {
        Job recurringCheckJob = BackgroundUtils.setUpPeriodicCalendarChecks(mFirebaseJobDispatcher);
        mFirebaseJobDispatcher.mustSchedule(recurringCheckJob);
    }

    public void doCalendarUpdate(){
        mFirebaseJobDispatcher.mustSchedule(BackgroundUtils.oneTimeCalendarUpdate(mFirebaseJobDispatcher));
    }

    public void analyzeEvents(){
        mFirebaseJobDispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(mFirebaseJobDispatcher));
    }

    public void checkLocationSettings(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest().setExpirationDuration(Constants.TIME_TEN_MINUTES)
                        .setFastestInterval(Constants.TIME_TEN_MINUTES)
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY));
        SettingsClient settingsClient = LocationServices.getSettingsClient(mActivity);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnSuccessListener(locationSettingsResponse -> {

        }).addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(mActivity, 1);
                } catch (IntentSender.SendIntentException sendEx) {
                    sendEx.printStackTrace();
                }
            }
        });
    }

    public void setupRateThisApp(){
        RateThisApp.onCreate(mActivity);
        RateThisApp.showRateDialogIfNeeded(mActivity);
    }

    public void setUpNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = mActivity.getString(R.string.notification_channel_name);
            String description = mActivity.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, channelName, importance);
            notificationChannel.setDescription(description);
            NotificationManager manager = mActivity.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }


    public void checkIfUpdateNeeded() {
        try {
            int currentVersion = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0).versionCode;
            AppApiService service = AppApiUtils.createService();
            service.queryVersionNumber(currentVersion).enqueue(new Callback<Version>() {
                @Override
                public void onResponse(Call<Version> call, Response<Version> response) {
                    Version v = response.body();
                    if (v == null) return;
                    int latestVersion = v.getVersion();
                    if (currentVersion < latestVersion) {
                        mActivity.showUpdateSnackbar();
                    } else if (mActivity.getString(R.string.version_invalid).equals(v.getMessage()) || v.getNeedsUpdate()) {
                        mFirebaseJobDispatcher.cancel(Constants.FIREBASE_JOB_SERVICE_CHECK_CALENDAR_CHANGED);
                        mFirebaseJobDispatcher.cancel(Constants.FIREBASE_JOB_SERVICE_SETUP_ACTIVITY_RECOG);
                        Intent intent = new Intent(mActivity, NeedUpdateActivity.class);
                        mActivity.startActivity(intent);
                    }
                }

                @Override
                public void onFailure(Call<Version> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentFragment(){
        NavDestination destination = mNavController.getCurrentDestination();
        if(destination == null) return -1;
        else return destination.getId();
    }

    public void navigateUp(){
        mNavController.navigateUp();
    }

    public void navigateToDestination(int id){
        mActivity.hideLoadingIcon();
        mNavController.navigate(id);

    }

}
