package com.aaronbrecher.neverlate.dependencyinjection;

import com.aaronbrecher.neverlate.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class})
public interface AppComponent {
    void inject(MainActivity mainActivity);
}
