package com.aaronbrecher.neverlate.ui.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
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
import com.aaronbrecher.neverlate.Utils.PermissionUtils;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;

import java.util.List;

import javax.inject.Inject;

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements ListItemClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    MainActivityViewModel mViewModel;
    private FragmentManager mFragmentManager;

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
        if(PermissionUtils.hasPermissions(this)){
            //TODO this will not be needed on a normal basis as the service will load all events into
            //the database, for the edge case that this doesn't happen will display a button to resync all
            //events for today...
            mViewModel.insertEvents(CalendarUtils.getCalendarEventsForToday(this));
        } else {
            PermissionUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
        }

        mFragmentManager = getSupportFragmentManager();
        if(getIntent().hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)){
            //load the details fragment for phone or tablet...
        } else {
            EventListFragment listFragment = new EventListFragment();
            mFragmentManager.beginTransaction().add(R.id.main_activity_list_fragment_container,
                    listFragment, Constants.EVENT_LIST_TAG)
                    .commit();
        }



        //this is for testing only, TODO change this to update a recyclerView with the events data
        mViewModel.getAllCurrentEvents().observe(this, new Observer<List<Event>>() {
            @Override
            public void onChanged(@Nullable List<Event> events) {
                for(Event event : events){
                    Log.i(TAG, "onChanged: " + event.getTitle() + " " + event.getLocation());
                }
            }
        });
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
        //TODO either replace the fragment with event details fragment (if do so need to work out back button)
        //or start new activity with details...
    }
}
