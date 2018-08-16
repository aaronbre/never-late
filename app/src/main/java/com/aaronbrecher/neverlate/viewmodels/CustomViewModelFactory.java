package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CustomViewModelFactory implements ViewModelProvider.Factory {

    private EventsRepository mEventsRepository;
    private GeofencesRepository mGeofencesRepository;
    private Application mApplication;

    @Inject
    public CustomViewModelFactory(EventsRepository eventsRepository, GeofencesRepository geofencesRepository, Application application) {
        this.mEventsRepository = eventsRepository;
        this.mGeofencesRepository = geofencesRepository;
        this.mApplication = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainActivityViewModel.class)) {
            return (T) new MainActivityViewModel(mEventsRepository,mGeofencesRepository, mApplication);
        } else if (modelClass.isAssignableFrom(DetailActivityViewModel.class)) {
            return (T) new DetailActivityViewModel(mEventsRepository, mGeofencesRepository);
        }
        else throw new IllegalArgumentException("ViewModel does not exist");
    }


}
