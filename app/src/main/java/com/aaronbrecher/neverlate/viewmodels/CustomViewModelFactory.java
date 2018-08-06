package com.aaronbrecher.neverlate.viewmodels;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.aaronbrecher.neverlate.database.EventsRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CustomViewModelFactory implements ViewModelProvider.Factory {
    private EventsRepository mEventsRepository;

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if(modelClass.isAssignableFrom(MainActivityViewModel.class)){
            return (T) new MainActivityViewModel(mEventsRepository);
        }else
            throw new IllegalArgumentException("ViewModel does not exist");
    }

    @Inject
    public CustomViewModelFactory(EventsRepository eventsRepository){
        this.mEventsRepository = eventsRepository;
    }
}
