package com.aaronbrecher.neverlate.dependencyinjection

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.BuildConfig
import com.aaronbrecher.neverlate.NeverLateApp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import javax.inject.Singleton

import dagger.Module
import dagger.Provides

@Module
class AppModule(private val mApplication: NeverLateApp) {

    @Provides
    @Singleton
    internal fun provideNeverLateApp(): NeverLateApp {
        return mApplication
    }

    @Provides
    @Singleton
    internal fun provideApp(): Application {
        return mApplication
    }

    @Provides
    @Singleton
    internal fun provideSharedPrefs(application: Application): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(application)
    }

    @Provides
    @Singleton
    internal fun provideFusedLocationProviderClient(application: Application): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(application)
    }

    @Provides
    @Singleton
    internal fun provideAppExecutor(): AppExecutors {
        return AppExecutors()
    }
}
