package com.aaronbrecher.neverlate.dependencyinjection;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;

import com.aaronbrecher.neverlate.database.EventsDao;
import com.aaronbrecher.neverlate.database.EventsDatabase;
import com.aaronbrecher.neverlate.database.EventsRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static com.aaronbrecher.neverlate.Constants.DATABASE_NAME;

@Module
public class RoomModule {
    Application mApplication;

    public RoomModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    EventsDatabase provideEventDatabase(Application application){
        return Room.databaseBuilder(application,
                EventsDatabase.class,
                DATABASE_NAME)
                .build();
    }

    @Provides
    @Singleton
    EventsDao provideEventDao(EventsDatabase database){
        return database.eventsDao();
    }

    @Provides
    @Singleton
    EventsRepository provideEventsRepository(EventsDao eventsDao){
        return new EventsRepository(eventsDao);
    }
}
