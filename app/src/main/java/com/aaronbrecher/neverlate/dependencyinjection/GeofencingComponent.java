package com.aaronbrecher.neverlate.dependencyinjection;

import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.geofencing.Geofencing;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class, GeofencingModule.class })
public interface GeofencingComponent {
    void inject(Geofencing geofencing);
    void inject(AwarenessFencesCreator creator);
}
