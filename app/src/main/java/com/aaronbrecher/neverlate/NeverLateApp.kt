package com.aaronbrecher.neverlate

import android.app.Activity
import android.app.Application
import android.os.Bundle

import com.aaronbrecher.neverlate.dependencyinjection.AppComponent
import com.aaronbrecher.neverlate.dependencyinjection.AppModule
import com.aaronbrecher.neverlate.dependencyinjection.DaggerAppComponent
import com.aaronbrecher.neverlate.dependencyinjection.RoomModule
import com.crashlytics.android.Crashlytics
import com.jakewharton.threetenabp.AndroidThreeTen

import io.fabric.sdk.android.Fabric

import android.app.Application.ActivityLifecycleCallbacks

class NeverLateApp : Application(), ActivityLifecycleCallbacks {

    lateinit var appComponent: AppComponent
        private set
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    var isInBackground = true
        private set

    override fun onCreate() {
        super.onCreate()
        app = this
        AndroidThreeTen.init(this)
        appComponent = DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .roomModule(RoomModule())
                .build()
        registerActivityLifecycleCallbacks(this)
        Fabric.with(this, Crashlytics())
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity?) {
        activity?.let {
            if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                // App enters foreground
                isInBackground = false
            }
        }
    }

    override fun onActivityStopped(activity: Activity?) {
        activity?.let {
            isActivityChangingConfigurations = it.isChangingConfigurations
            if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                // App enters background
                isInBackground = true
            }
        }
    }

    override fun onActivityPaused(activity: Activity?) {
    }

    override fun onActivityResumed(activity: Activity?) {
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {

    }

    override fun onActivityDestroyed(activity: Activity?) {

    }

    companion object {
        lateinit var app: NeverLateApp
            private set
    }
}