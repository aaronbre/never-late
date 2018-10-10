package com.aaronbrecher.neverlate.dependencyinjection;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.BuildConfig;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.maps.GeoApiContext;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {
    private NeverLateApp mApplication;

    public AppModule(NeverLateApp application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    NeverLateApp provideNeverLateApp(){
        return mApplication;
    }

    @Provides
    @Singleton
    Application provideApp(){
        return mApplication;
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPrefs(Application application){
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides
    @Singleton
    FusedLocationProviderClient provideFusedLocationProviderClient(Application application){
        return LocationServices.getFusedLocationProviderClient(application);
    }

    @Provides
    @Singleton
    AppExecutors provideAppExecutor(){
        return new AppExecutors();
    }

    @Provides
    GeoApiContext provideGeoApiContext(){
        return new GeoApiContext().setApiKey(BuildConfig.GOOGLE_API_KEY);
    }

}
