package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MainActivityViewModel extends BaseViewModel {
    private MutableLiveData<Event> mEvent;
    private MutableLiveData<Boolean> mShouldShowAllEvents;
    //this field is to compare previous location so as not to do
    //additional api call on orientation change
    private List<Event> mPreviousLocationList = new ArrayList<>();

    //this field differs from the getAllCurrentEvents as the db is not location aware
    //this field will contain the location info as well (distance and time to travel)
    private MutableLiveData<List<Event>> mEventsWithLocation;

    @Inject
    public MainActivityViewModel(EventsRepository eventsRepository, Application application, AppExecutors appExecutors) {
        super(eventsRepository, application, appExecutors);
    }

    //insert the events async using a simple async task
    public void insertEvents(List<Event> events) {
        mAppExecutors.diskIO().execute(()-> mEventsRepository.insertAll(events));
    }

    public LiveData<List<Event>> getAllCurrentEvents() {
        return mEventsRepository.queryAllCurrentTrackedEvents();
    }

    public LiveData<List<Event>> getAllEvents() {
        return mEventsRepository.queryEventsNoLocation();
    }

    public MutableLiveData<Event> getEvent() {
        if (mEvent == null)
            mEvent = new MutableLiveData<>();
        return mEvent;
    }

    public void setEvent(Event event) {
        if (mEvent == null) {
            mEvent = new MutableLiveData<>();
        }
        mEvent.postValue(event);
    }

    public MutableLiveData<Boolean> getShouldShowAllEvents() {
        if(mShouldShowAllEvents == null) mShouldShowAllEvents = new MutableLiveData<>();
        return mShouldShowAllEvents;
    }

    public void setShouldShowAllEvents(boolean bool){
        if(mShouldShowAllEvents == null) mShouldShowAllEvents = new MutableLiveData<>();
        mShouldShowAllEvents.postValue(bool);
    }

    public void updateEvent(Event event){
        mAppExecutors.diskIO().execute(()-> mEventsRepository.updateEvents(event));
    }

    public void setShowAllEvents(){

    }

    public void deleteAllEvents(){
        mEventsRepository.deleteAllEvents();
    }

    public void setSnoozeForTime(long endTime){
        mAppExecutors.diskIO().execute(() -> deleteAllEvents());
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(mApplication));
        dispatcher.cancelAll();
        dispatcher.mustSchedule(BackgroundUtils.endSnoozeJob(dispatcher, endTime));
    }

    public void rescheduleAllJobs(){
        FirebaseJobDispatcher jobDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(mApplication));
        jobDispatcher.mustSchedule(BackgroundUtils.setUpPeriodicCalendarChecks(jobDispatcher));
        jobDispatcher.mustSchedule(BackgroundUtils.setUpActivityRecognitionJob(jobDispatcher));
    }
}
