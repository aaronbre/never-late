package com.aaronbrecher.neverlate.dependencyinjection;

import android.app.Application;

import com.google.android.gms.location.GeofencingClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class GeofencingModule {

    public GeofencingModule() {
    }

    @Provides
    @Singleton
    GeofencingClient providesGeofencingClient(Application application){
        return new GeofencingClient(application);
    }
}
