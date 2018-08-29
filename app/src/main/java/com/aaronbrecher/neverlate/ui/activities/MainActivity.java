package com.aaronbrecher.neverlate.ui.activities;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.Utils.PermissionUtils;
import com.aaronbrecher.neverlate.backgroundservices.CalendarAlarmService;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import java.util.Calendar;
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
        if (!PermissionUtils.hasPermissions(this)) {
            PermissionUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
        } else {
            setUpAlarmManager();
        }

        mViewModel.getAllCurrentEvents().observe(this, new Observer<List<Event>>() {
            @Override
            public void onChanged(@Nullable List<Event> events) {
                Log.i(TAG, "onChanged: was called");
                locateDeviceAndLoadUi(events);
                if (mViewModel.getEvent().getValue() == null
                        && events != null
                        && events.size() != 0) mViewModel.setEvent(events.get(0));
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void locateDeviceAndLoadUi(final List<Event> events) {
        toggleListVisibility();
        mLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                mViewModel.setEventsWithLocation(events, location);
                loadFragment();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mViewModel.setEventsWithLocation(events, null);
                loadFragment();
            }
        });
    }


    private void loadFragment() {
        toggleListVisibility();
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
            if (PermissionUtils.verifyPermissions(grantResults)) {
                setUpAlarmManager();
//              mViewModel.insertEvents(CalendarUtils.getCalendarEventsForToday(this));
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
            mViewModel.insertEvents(CalendarUtils.getCalendarEventsForToday(this));
        }
        return super.onOptionsItemSelected(item);
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

    private void toggleListVisibility() {
        if (mListContainer.getVisibility() == View.VISIBLE) {
            mListContainer.setVisibility(View.GONE);
            if (getResources().getBoolean(R.bool.is_tablet))
                mDetailContainer.setVisibility(View.GONE);
            Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
            mProgressSpinner.setVisibility(View.VISIBLE);
            mProgressSpinner.startAnimation(rotate);
        } else if (mListContainer.getVisibility() == View.GONE) {
            mProgressSpinner.clearAnimation();
            mProgressSpinner.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
            if (getResources().getBoolean(R.bool.is_tablet))
                mDetailContainer.setVisibility(View.VISIBLE);
        }
    }

}
