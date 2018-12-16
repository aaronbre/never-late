package com.aaronbrecher.neverlate;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.dependencyinjection.AppModule;
import com.aaronbrecher.neverlate.dependencyinjection.DaggerAppComponent;
import com.aaronbrecher.neverlate.dependencyinjection.RoomModule;
import com.crashlytics.android.Crashlytics;
import com.jakewharton.threetenabp.AndroidThreeTen;

import io.fabric.sdk.android.Fabric;

import static android.app.Application.*;

public class NeverLateApp extends Application implements ActivityLifecycleCallbacks {
    private static NeverLateApp app;
    private AppComponent mAppComponent;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private boolean isInBackground = true;

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
        registerActivityLifecycleCallbacks(this);
    }

    public static NeverLateApp getApp(){
        return app;
    }

    public AppComponent getAppComponent() {
        return mAppComponent;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App enters foreground
            isInBackground = false;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
            isInBackground = true;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public boolean isInBackground() {
        return isInBackground;
    }
}
