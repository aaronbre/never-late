package com.aaronbrecher.neverlate.dependencyinjection;

import android.content.SharedPreferences;

import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class})
public interface AppComponent {
    void inject(MainActivity mainActivity);
    void inject(EventListFragment fragment);
    void inject(EventDetailActivity detailActivity);
    void inject(EventDetailFragment fragment);
    SharedPreferences getSharedPreferences();
}
