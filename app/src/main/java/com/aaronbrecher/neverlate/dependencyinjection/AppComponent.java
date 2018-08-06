package com.aaronbrecher.neverlate.dependencyinjection;

import android.content.SharedPreferences;

import com.aaronbrecher.neverlate.ui.activities.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class})
public interface AppComponent {
    void inject(MainActivity mainActivity);
    SharedPreferences getSharedPreferences();
}
