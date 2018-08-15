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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

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

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements ListItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;

    MainActivityViewModel mViewModel;
    private FragmentManager mFragmentManager;
    private FrameLayout mListContainer;
    private Geofencing mGeofencing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpNotificationChannel();
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory)
                .get(MainActivityViewModel.class);
        mFragmentManager = getSupportFragmentManager();
        mListContainer = findViewById(R.id.main_activity_list_fragment_container);
        if (!PermissionUtils.hasPermissions(this)) {
            PermissionUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
        }
        mViewModel.getAllCurrentEvents().observe(this, new Observer<List<Event>>() {
            @Override
            public void onChanged(@Nullable List<Event> events) {
                Log.i(TAG, "onChanged: was called");
                locateDeviceAndLoadUi(events);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void locateDeviceAndLoadUi(final List<Event> events) {
        //TODO add a progress spinner and show here - remove in loadFragment()
        mListContainer.setVisibility(View.GONE);
        mLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //This currently works as expected TODO possibly change this to loader to deal with lifecycle events...
                new AsyncTask<Pair<List<Event>, Location>, Void, Void>(){

                    @Override
                    protected Void doInBackground(Pair<List<Event>, Location>... pairs) {
                        Pair<List<Event>, Location> pair = pairs[0];
                        MainActivity.this.addLocationToEvents(pair.second, pair.first);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        loadData(events);
                        loadFragment();
                    }
                }.execute(new Pair<List<Event>, Location>(events, location));
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
        //TODO currently does this on orientation change.. fix
        for (Event event : events) {
            //set the distance to the event using the location
            event.setDistance(getDistance(location, event.getLocation()));
            //get the travel time to the event using the google directions api this blocks the
            //main UI thread as all information is needed to update the viewModel
            //TODO see if there is a better way...
            DirectionsApiRequest apiRequest = DirectionsUtils.getDirectionsApiRequest(
                    LocationUtils.latlngFromAddress(this, event.getLocation()),
                    LocationUtils.locationToLatLng(location));
            try {
                DirectionsResult result = apiRequest.await();
                event.setTimeTo(result.routes[0].legs[0].duration.humanReadable);
            } catch (ApiException | InterruptedException | IOException e) {
                e.printStackTrace();
            }

        }
        return new ArrayList<>(events);
    }

    private void loadData(List<Event> events) {
        //load the updated events with the location aware information to the VM
        mViewModel.setEventsWithLocation(events);
        //TODO this is for testing only Geofencing will be handled by job service
        Geofencing geofencing = new Geofencing(MainActivity.this, events, mSharedPreferences);
        List<GeofenceModel> geofenceModels = geofencing.setUpGeofences();
    }

    private String getDistance(Location location, String destinationAddress) {
        LatLng latLng = LocationUtils.latlngFromAddress(this, destinationAddress);
        Location destination = LocationUtils.latlngToLocation(latLng);
        return LocationUtils.getDistance(location, destination);
    }

    private void loadFragment() {
        mListContainer.setVisibility(View.VISIBLE);
        if (getIntent().hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)) {
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
                mViewModel.insertEvents(CalendarUtils.getCalendarEventsForToday(this));
            } else {
                PermissionUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.main_activity_menu_sync) {
            mViewModel.insertEvents(CalendarUtils.getCalendarEventsForToday(this));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(Parcelable event) {
        if (getResources().getBoolean(R.bool.is_tablet)) {
            //replace event in viewmodel to update the fragment
        } else {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
            startActivity(intent);
        }
    }

}
