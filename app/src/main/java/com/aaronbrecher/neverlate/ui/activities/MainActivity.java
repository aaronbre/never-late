package com.aaronbrecher.neverlate.ui.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.database.Cursor;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.PermissionUtils;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;

import java.util.List;

import javax.inject.Inject;

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    MainActivityViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView textView = findViewById(R.id.main_text);
        setUpNotificationChannel();
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mViewModel = ViewModelProviders.of(this,mViewModelFactory)
                .get(MainActivityViewModel.class);
        if(PermissionUtils.hasPermissions(this)){
            //TODO this will not be needed on a normal basis as the service will load all events into
            //the database, for the edge case that this doesn't happen will display a button to reload all
            //events for today...
            Cursor cursor = CalendarUtils.getCalendarEventsForToday(this);
            while (cursor.moveToNext()){
                Event event = getEvent(cursor);
                mViewModel.insertEvent(event);
            }
        } else{
            PermissionUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
        }

        //this is for testing only, TODO change this to update a recyclerView with the events data
        mViewModel.getAllEvents().observe(this, new Observer<List<Event>>() {
            @Override
            public void onChanged(@Nullable List<Event> events) {
                for(Event event : events){
                    Log.i(TAG, "onChanged: " + event.getTitle() + " " + event.getLocation());
                    textView.append(event.getTitle() + " " + event.getLocation());
                }
            }
        });
    }

    private Event getEvent(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(BaseColumns._ID);
        int titleIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_TITLE);
        int descriptionIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DESCRIPTION);
        int locationIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_EVENT_LOCATION);
        int startIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DTSTART);
        int endIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_DTEND);
        int calendarIdIndex = cursor.getColumnIndex(Constants.CALENDAR_EVENTS_CALENDAR_ID);

        Event event = new Event();
        event.setId(cursor.getInt(idIndex));
        event.setTitle(cursor.getString(titleIndex));
        event.setDescription(cursor.getString(descriptionIndex));
        event.setLocation(cursor.getString(locationIndex));
        event.setStartTime(cursor.getLong(startIndex));
        event.setEndTime(cursor.getLong(endIndex));
        event.setCalendarId(cursor.getLong(calendarIdIndex));
        return event;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            //if permissions are granted for the first time, assume data was not loaded into room and
            //do so now...
            if (PermissionUtils.verifyPermissions(grantResults)) {
                Cursor cursor = CalendarUtils.getCalendarEventsForToday(this);
                while (cursor.moveToNext()){
                    Event event = getEvent(cursor);
                    mViewModel.insertEvent(event);
                }
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
}
