package com.aaronbrecher.neverlate.database;

import androidx.lifecycle.LiveData;

import com.aaronbrecher.neverlate.models.EventCompatibility;

import java.util.List;

import javax.inject.Inject;

public class EventCompatibilityRepository {
    EventCompatibilityDao mDao;

    @Inject
    public EventCompatibilityRepository(EventCompatibilityDao dao){
        this.mDao = dao;
    }

    public void insertAll(List<EventCompatibility> list){
        mDao.insertAll(list);
    }

    public LiveData<List<EventCompatibility>> queryCompatibility(){
        return mDao.getAllCompatibilities();
    }

    public void deleteAll() {
        mDao.deleteAllEvents();
    }
}
