package com.aaronbrecher.neverlate.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

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
