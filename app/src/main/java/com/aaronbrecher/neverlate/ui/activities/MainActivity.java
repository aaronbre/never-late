package com.aaronbrecher.neverlate.ui.activities;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.PermissionUtils;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.work.WorkManager;

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements ListItemClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;

    MainActivityViewModel mViewModel;
    private FragmentManager mFragmentManager;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;

    private Geofencing mGeofencing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpNotificationChannel();
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mViewModel = ViewModelProviders.of(this,mViewModelFactory)
                .get(MainActivityViewModel.class);
        mFragmentManager = getSupportFragmentManager();
        if(PermissionUtils.hasPermissions(this)){
            final List<Event> events = CalendarUtils.getCalendarEventsForToday(this);
            locateDevice(events);
        } else {
            PermissionUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
        }

        loadFragment();
    }

    @SuppressLint("MissingPermission")
    private void locateDevice(final List<Event> events) {
        mLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                List<Event> eventsWithLocation = addLocationToEvents(location, events);
                loadData(eventsWithLocation);
                loadFragment();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                loadData(events);
                loadFragment();
            }
        });
    }

    private List<Event> addLocationToEvents(Location location, List<Event> events) {
        for(Event event : events){
            //set the distance to the event using the location
            event.setDistance(getDistance(location, event.getLocation()));
            DirectionsApiRequest apiRequest = DirectionsUtils.getDirectionsApiRequest(
                    LocationUtils.latlngFromAddress(this, event.getLocation()),
                    LocationUtils.locationToLatLng(location));
            try {
                DirectionsResult result = apiRequest.await();
                event.setTimeTo(result.routes[0].legs[0].duration.humanReadable);
            } catch (ApiException | InterruptedException | IOException e) { e.printStackTrace(); }

        }
        return new ArrayList<>(events);
    }

    private void loadData(List<Event> events) {
        mViewModel.insertEvents(events);
        Geofencing geofencing = new Geofencing(MainActivity.this, events, mSharedPreferences);
        List<GeofenceModel> geofenceModels = geofencing.setUpGeofences();
    }

    private String getDistance(Location location, String destinationAddress){
        LatLng latLng = LocationUtils.latlngFromAddress(this, destinationAddress);
        Location destination = LocationUtils.latlngToLocation(latLng);
        return LocationUtils.getDistance(location, destination);
    }

    private void loadFragment() {
        if(getIntent().hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)){
            //load the details fragment for phone or tablet...
        } else {
            EventListFragment listFragment = new EventListFragment();
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
            if (PermissionUtils.verifyPermissions(grantResults)) {
                List<Event> events = CalendarUtils.getCalendarEventsForToday(this);
                mViewModel.insertEvents(events);
            } else {
                PermissionUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
                // TODO change this to Show image showing error with button to rerequest permissions...
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setUpNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence channelName = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, channelName, importance);
            notificationChannel.setDescription(description);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onListItemClick(Parcelable event) {
        if(getResources().getBoolean(R.bool.is_tablet)){
            //replace event in viewmodel to update the fragment
        } else {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
            startActivity(intent);
        }
    }
}
