package com.aaronbrecher.neverlate.backgroundservices;

import android.content.SharedPreferences;
import android.util.Log;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatiblity;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.util.List;

import javax.inject.Inject;

public class AnaylizeEventsJobService extends JobService {
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors mAppExecutors;

    private JobParameters mJobParameters;
    private List<Event> mEventList;
    private List<EventCompatiblity> mEventCompatiblities;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        mJobParameters = job;
        mAppExecutors.diskIO().execute(this::doWork);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    private void doWork(){
        mEventList = mEventsRepository.queryAllCurrentEventsSync();
        for(int i = 0; i < mEventList.size(); i++){
            isWithinRange(mEventList.get(i), mEventList.get(i+1));
        }
    }

    private EventCompatiblity isWithinRange(Event event, Event event1) {
        EventCompatiblity compatiblity = new EventCompatiblity();
        return compatiblity;
    }
}
