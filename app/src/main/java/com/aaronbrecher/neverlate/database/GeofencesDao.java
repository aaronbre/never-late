package com.aaronbrecher.neverlate.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.aaronbrecher.neverlate.models.GeofenceModel;

import java.util.List;

@Dao
public interface GeofencesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GeofenceModel> geofences);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertFence(GeofenceModel item);

    @Query("SELECT * FROM geofences WHERE requestKey = :requestKey")
    LiveData<GeofenceModel> getGeofenceByKey(String requestKey);

    @Query("DELETE FROM geofences WHERE requestKey = :requestKey")
    void deleteGeofenceWithKey(String requestKey);
}
