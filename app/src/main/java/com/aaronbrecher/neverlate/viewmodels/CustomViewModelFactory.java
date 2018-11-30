package com.aaronbrecher.neverlate.viewmodels;

import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository;
import com.aaronbrecher.neverlate.database.EventsRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CustomViewModelFactory implements ViewModelProvider.Factory {

    private EventsRepository mEventsRepository;
    private Application mApplication;
    private AppExecutors mAppExecutors;

    @Inject
    public CustomViewModelFactory(EventsRepository eventsRepository, EventCompatibilityRepository compatabilityRepository,
                                  Application application, AppExecutors appExecutors) {
        this.mEventsRepository = eventsRepository;
        this.mApplication = application;
        this.mAppExecutors = appExecutors;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainActivityViewModel.class)) {
            return (T) new MainActivityViewModel(mEventsRepository,mApplication, mAppExecutors);
        } else if (modelClass.isAssignableFrom(DetailActivityViewModel.class)) {
            return (T) new DetailActivityViewModel(mEventsRepository, mAppExecutors);
        }
        else throw new IllegalArgumentException("ViewModel does not exist");
    }


}
