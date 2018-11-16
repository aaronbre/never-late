package com.aaronbrecher.neverlate.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;

import com.aaronbrecher.neverlate.models.EventCompatibility;

import java.util.List;

@Dao
public interface EventCompatabilityDao {
    @Query("DELETE FROM compatibility")
    void deleteAllEvents();

    @Query("SELECT * FROM compatibility")
    LiveData<List<EventCompatibility>> checkAllCompatability();
}
