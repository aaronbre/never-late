package com.aaronbrecher.neverlate.ui.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.aaronbrecher.neverlate.BuildConfig;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.interfaces.LocationCallback;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;
import com.aaronbrecher.neverlate.ui.fragments.NoEventsFragment;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.GeoApiContext;

import java.util.List;

import javax.inject.Inject;

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements ListItemClickListener, LocationCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;

    private GeoApiContext mGeoApiContext = new GeoApiContext().setApiKey(BuildConfig.GOOGLE_API_KEY);
    private List<Event> mEventList;
    MainActivityViewModel mViewModel;
    private FragmentManager mFragmentManager;
    private FrameLayout mListContainer;
    private ImageView mProgressSpinner;
    private FrameLayout mDetailContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);

        setUpNotificationChannel();
        mViewModel = ViewModelProviders.of(this, mViewModelFactory)
                .get(MainActivityViewModel.class);
        mFragmentManager = getSupportFragmentManager();
        mListContainer = findViewById(R.id.main_activity_list_fragment_container);
        mDetailContainer = findViewById(R.id.main_activity_detail_fragment);
        mProgressSpinner = findViewById(R.id.progress_spinner);
        if (!SystemUtils.hasPermissions(this)) {
            SystemUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
        } else {
            setUpAlarmManager();
        }
        checkLocationSettings();
        mViewModel.getAllCurrentEvents().observe(this, new Observer<List<Event>>() {
            @Override
            public void onChanged(@Nullable List<Event> events) {
                if(events == null || events.size() < 1){
                    loadNoEventsFragment();
                }
                else {
                    Log.i(TAG, "onChanged: was called");
                    loadListFragment();
                    if (mViewModel.getEvent().getValue() == null) mViewModel.setEvent(events.get(0));
                }
            }
        });
    }

    private void loadNoEventsFragment() {
        NoEventsFragment fragment = new NoEventsFragment();
        mFragmentManager.beginTransaction().replace(R.id.main_activity_list_fragment_container, fragment).commit();
    }

    private void loadListFragment() {
        EventListFragment listFragment = new EventListFragment();
        if (getResources().getBoolean(R.bool.is_tablet)) {
            EventDetailFragment eventDetailFragment = new EventDetailFragment();
            mFragmentManager.beginTransaction().
                    replace(R.id.main_activity_list_fragment_container,
                            listFragment, Constants.EVENT_LIST_TAG).
                    replace(R.id.main_activity_detail_fragment,
                            eventDetailFragment, Constants.EVENT_DETAIL_FRAGMENT_TAG)
                    .commit();
        } else {
            mFragmentManager.beginTransaction().replace(R.id.main_activity_list_fragment_container,
                    listFragment, Constants.EVENT_LIST_TAG)
                    .commit();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            //if permissions are granted for the first time, assume data was not loaded into room and
            //do so now...
            if (SystemUtils.verifyPermissions(grantResults)) {
                setUpAlarmManager();
//              mViewModel.insertEvents(CalendarUtils.getCalendarEventsForToday(this));
            } else {
                SystemUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
                // TODO change this to Show image showing error with button to rerequest permissions...
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setUpNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, channelName, importance);
            notificationChannel.setDescription(description);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    //This will only set the alarm if it wasn't already set there will be a
    //seperate broadcast reciever to schedule alarm after boot...
    private void setUpAlarmManager() {
        boolean alarmSet = false;
        if (mSharedPreferences.contains(Constants.ALARM_STATUS_KEY)) {
            alarmSet = mSharedPreferences.getBoolean(Constants.ALARM_STATUS_KEY, true);
        }
        if (alarmSet) return;

        boolean wasSet = BackgroundUtils.setAlarmManager(this);
        if (wasSet) mSharedPreferences.edit().putBoolean(Constants.ALARM_STATUS_KEY, true).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.main_activity_menu_sync) {
            if (SystemUtils.isConnected(this)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mEventList = CalendarUtils.getCalendarEventsForToday(MainActivity.this);
                        BackgroundUtils.getLocation(MainActivity.this, MainActivity.this, mLocationProviderClient);
                    }
                }, "MainActivityRefreshThread").start();
            } else {
                showNoConnectionSnackbar();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNoConnectionSnackbar() {
        Snackbar.make(mListContainer, R.string.refresh_error_no_connection, Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.launch_data_settings_button), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.setComponent(
                                new ComponentName(
                                        "com.android.settings",
                                        "com.android.settings.Settings$DataUsageSummaryActivity"
                                ));
                        startActivity(intent);
                    }
                })
                .show();
    }

    @Override
    public void onListItemClick(Parcelable event) {
        if (getResources().getBoolean(R.bool.is_tablet)) {
            mViewModel.setEvent((Event) event);
        } else {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
            startActivity(intent);
        }
    }

    @Override
    public void getLocationSuccessCallback(final Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mEventList == null || mEventList.size() < 1) return;
                DirectionsUtils.addDistanceInfoToEventList(mGeoApiContext, mEventList, location);
                mViewModel.deleteAllEvents();
                mViewModel.insertEvents(mEventList);
                mSharedPreferences.edit().putString(Constants.USER_LOCATION_PREFS_KEY, LocationUtils.locationToGsonString(location)).apply();
                AwarenessFencesCreator creator = new AwarenessFencesCreator.Builder(mEventList).build();
                creator.buildAndSaveFences();
            }
        }, "MainActivitylocationThread").start();
    }

    @Override
    public void getLocationFailedCallback() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //mViewModel.deleteAllEvents();
                mViewModel.insertEvents(mEventList);
            }
        }).start();
        //TODO find out why it failed? Location is needed for the app to operate...
    }

    private void checkLocationSettings(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest().setExpirationDuration(Constants.TIME_TEN_MINUTES)
                        .setFastestInterval(Constants.TIME_TEN_MINUTES)
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY));
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(e instanceof ResolvableApiException){
                    try{
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,1);
                    }catch (IntentSender.SendIntentException sendEx){
                        Log.e(TAG, "onFailure: " + sendEx );
                    }
                }
            }
        });
    }

//    /**
//     * handle showing a loader image while loading data
//     */
//    private void toggleListVisibility() {
//        if (mListContainer.getVisibility() == View.VISIBLE) {
//            mListContainer.setVisibility(View.GONE);
//            if (getResources().getBoolean(R.bool.is_tablet))
//                mDetailContainer.setVisibility(View.GONE);
//            Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
//            mProgressSpinner.setVisibility(View.VISIBLE);
//            mProgressSpinner.startAnimation(rotate);
//        } else if (mListContainer.getVisibility() == View.GONE) {
//            mProgressSpinner.clearAnimation();
//            mProgressSpinner.setVisibility(View.GONE);
//            mListContainer.setVisibility(View.VISIBLE);
//            if (getResources().getBoolean(R.bool.is_tablet))
//                mDetailContainer.setVisibility(View.VISIBLE);
//        }
//    }
}
