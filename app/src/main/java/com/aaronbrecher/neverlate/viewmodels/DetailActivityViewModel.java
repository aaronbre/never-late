package com.aaronbrecher.neverlate.viewmodels;

import androidx.lifecycle.MutableLiveData;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.models.Event;

import java.util.ArrayList;

import javax.inject.Inject;

public class DetailActivityViewModel extends BaseViewModel {
    private MutableLiveData<Event> mEvent;

    @Inject
    public DetailActivityViewModel(EventsRepository eventsRepository, AppExecutors appExecutors) {
        super(eventsRepository, null, appExecutors);
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

    public void updateEvent(Event event) {
        mAppExecutors.diskIO().execute(() -> mEventsRepository.updateEvents(event));
    }

    public void resetFenceForEvent(Event event){
        ArrayList<Event> events = new ArrayList<>();
        events.add(event);
        AwarenessFencesCreator creator = new AwarenessFencesCreator.Builder(events).build();
        creator.buildAndSaveFences();
    }

    public void removeGeofenceForEvent(Event event){
        AwarenessFencesCreator creator = new AwarenessFencesCreator.Builder(null).build();
        creator.removeFences(event);
    }

}
