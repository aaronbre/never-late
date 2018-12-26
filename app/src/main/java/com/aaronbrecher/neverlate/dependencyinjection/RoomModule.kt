package com.aaronbrecher.neverlate.dependencyinjection

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.database.EventCompatibilityDao
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository
import com.aaronbrecher.neverlate.database.EventsDao
import com.aaronbrecher.neverlate.database.EventsDatabase
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.viewmodels.CustomViewModelFactory

import javax.inject.Singleton

import dagger.Module
import dagger.Provides

import com.aaronbrecher.neverlate.Constants.DATABASE_NAME

@Module
class RoomModule {

    @Provides
    @Singleton
    internal fun provideEventDatabase(application: Application): EventsDatabase {
        return Room.databaseBuilder(application,
                EventsDatabase::class.java,
                DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    @Singleton
    internal fun provideEventDao(database: EventsDatabase): EventsDao {
        return database.eventsDao()
    }

    @Provides
    @Singleton
    internal fun provideEventsRepository(eventsDao: EventsDao): EventsRepository {
        return EventsRepository(eventsDao)
    }

    @Provides
    @Singleton
    internal fun provideCompatablityDao(database: EventsDatabase): EventCompatibilityDao {
        return database.compatabilityDao()
    }

    @Provides
    @Singleton
    internal fun provideCompatablityRepository(compatabilityDao: EventCompatibilityDao): EventCompatibilityRepository {
        return EventCompatibilityRepository(compatabilityDao)
    }

    @Provides
    @Singleton
    internal fun provideViewModelFactory(eventsRepository: EventsRepository,
                                         compatabilityRepository: EventCompatibilityRepository, application: Application, appExecutors: AppExecutors): ViewModelProvider.Factory {
        return CustomViewModelFactory(eventsRepository, compatabilityRepository, application, appExecutors)
    }
}
