package com.aaronbrecher.neverlate.backgroundservices;

import android.content.SharedPreferences;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.models.Event;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.util.List;

import javax.inject.Inject;

/**
 * This services purpose is to schedule the initial job at midnight as well
 * as to schedule the recurring job to be performed each day
 */
public class RetrieveCalendarEventsInitialJobService extends JobService {
    @Inject
    EventsRepository mEventsRepository;

    @Inject
    SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doJob(params);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public void doJob(JobParameters params){
        List<Event> events = CalendarUtils.getCalendarEventsForToday(this);
        mEventsRepository.insertAll(events);
        Geofencing geofencing = new Geofencing(this, events, mSharedPreferences);
        geofencing.setUpGeofences();
        scheduleRecurringJob();
        jobFinished(params, false);
    }

    private void scheduleRecurringJob() {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));

    }
}
