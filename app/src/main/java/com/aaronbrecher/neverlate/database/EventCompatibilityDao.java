package com.aaronbrecher.neverlate.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatibility;

import java.util.List;

@Dao
public interface EventCompatibilityDao {
    @Query("DELETE FROM compatibility")
    void deleteAllEvents();

    @Query("SELECT * FROM compatibility")
    LiveData<List<EventCompatibility>> getAllCompatibilities();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EventCompatibility> list);
}
