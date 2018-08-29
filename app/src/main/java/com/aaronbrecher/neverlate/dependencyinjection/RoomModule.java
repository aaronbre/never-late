package com.aaronbrecher.neverlate.dependencyinjection;

import android.app.Application;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;

import com.aaronbrecher.neverlate.database.EventsDao;
import com.aaronbrecher.neverlate.database.EventsDatabase;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesDao;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
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
    GeofencesDao provideGeofenceDao(EventsDatabase database) {
        return database.geofencesDao();
    }

    @Provides
    @Singleton
    GeofencesRepository provideGeofenceRepository(GeofencesDao geofencesDao) {
        return new GeofencesRepository(geofencesDao);
    }

    @Provides
    @Singleton
    ViewModelProvider.Factory provideViewModelFactory(EventsRepository eventsRepository, GeofencesRepository geofencesRepository, Application application) {
        return new CustomViewModelFactory(eventsRepository, geofencesRepository, application);
    }
}
