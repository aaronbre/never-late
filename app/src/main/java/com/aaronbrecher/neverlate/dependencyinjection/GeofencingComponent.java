package com.aaronbrecher.neverlate.dependencyinjection;

import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class, GeofencingModule.class })
public interface GeofencingComponent {
    void inject(AwarenessFencesCreator creator);
}
