package com.aaronbrecher.neverlate;

import android.app.Application;

import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.dependencyinjection.AppModule;
import com.aaronbrecher.neverlate.dependencyinjection.DaggerAppComponent;
import com.aaronbrecher.neverlate.dependencyinjection.RoomModule;
import com.crashlytics.android.Crashlytics;
import com.jakewharton.threetenabp.AndroidThreeTen;

import io.fabric.sdk.android.Fabric;

public class NeverLateApp extends Application {
    private static NeverLateApp app;
    private AppComponent mAppComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        app = this;
        AndroidThreeTen.init(this);
        mAppComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .roomModule(new RoomModule())
                .build();
    }

    public static NeverLateApp getApp(){
        return app;
    }

    public AppComponent getAppComponent() {
        return mAppComponent;
    }

}
