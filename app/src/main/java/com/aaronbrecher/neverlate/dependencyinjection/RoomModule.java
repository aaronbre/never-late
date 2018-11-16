package com.aaronbrecher.neverlate.dependencyinjection;

import android.app.Application;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.persistence.room.Room;

import com.aaronbrecher.neverlate.database.EventCompatabilityDao;
import com.aaronbrecher.neverlate.database.EventCompatabilityRepository;
import com.aaronbrecher.neverlate.database.EventsDao;
import com.aaronbrecher.neverlate.database.EventsDatabase;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.viewmodels.CustomViewModelFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static com.aaronbrecher.neverlate.Constants.DATABASE_NAME;

@Module
public class RoomModule {

    public RoomModule() {

    }

    @Provides
    @Singleton
    EventsDatabase provideEventDatabase(Application application) {
        return Room.databaseBuilder(application,
                EventsDatabase.class,
                DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    EventsDao provideEventDao(EventsDatabase database) {
        return database.eventsDao();
    }

    @Provides
    @Singleton
    EventsRepository provideEventsRepository(EventsDao eventsDao) {
        return new EventsRepository(eventsDao);
    }

    @Provides
    @Singleton
    EventCompatabilityDao provideCompatablityDao(EventsDatabase database) {
        return database.compatabilityDao();
    }

    @Provides
    @Singleton
    EventCompatabilityRepository provideCompatablityRepository(EventCompatabilityDao compatabilityDao) {
        return new EventCompatabilityRepository(compatabilityDao);
    }

    @Provides
    @Singleton
    ViewModelProvider.Factory provideViewModelFactory(EventsRepository eventsRepository, EventCompatabilityRepository compatabilityRepository, Application application) {
        return new CustomViewModelFactory(eventsRepository, compatabilityRepository, application);
    }
}
