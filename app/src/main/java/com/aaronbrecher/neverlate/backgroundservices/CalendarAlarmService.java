package com.aaronbrecher.neverlate.backgroundservices;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import java.util.List;

import javax.inject.Inject;

public class CalendarAlarmService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    GeofencesRepository mGeofencesRepository;

    @Inject
    SharedPreferences mSharedPreferences;


    public CalendarAlarmService() {
        super("Calendar-alarm-service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((NeverLateApp) getApplication()).getAppComponent()
                .inject(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent != null){
            String action = intent.getAction();
            if(action.equals(Constants.ACTION_ADD_CALENDAR_EVENTS)){
                mEventsRepository.deleteAllEvents();
                List<Event> eventList = CalendarUtils.getCalendarEventsForToday(this);
                mEventsRepository.insertAll(eventList);
                initializeGeofences(eventList);
                FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
                Job job = createJob(dispatcher);
                dispatcher.mustSchedule(job);
            }
        }
    }

    /**
     * Create a job to repeat every 15 minutes to update the geofences ultimately
     * will either use the default 15 mins or user provided time from prefs
     * TODO possibly move this to Utils to use multiple places...
     * @param dispatcher a dipatcher to create the job
     * @return a FirebaseJob to schedule via firebase dispatcher
     */
    private Job createJob(FirebaseJobDispatcher dispatcher) {
        //TODO change trigger to check for user preference...
        return dispatcher.newJobBuilder()
                .setService(GeofenceJobService.class)
                .setTag(Constants.FIREBASE_JOB_SERVICE_UPDATE_GEOFENCES)
                .setTrigger(Trigger.executionWindow(15*60, 20*60))
                .setRecurring(true)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();
    }

    private void initializeGeofences(List<Event> eventList){
        Geofencing geofencing = new Geofencing(this, eventList, mSharedPreferences);
        List<GeofenceModel> geofenceModels = geofencing.setUpGeofences();
    }
}
