package com.aaronbrecher.neverlate.database;

import android.arch.lifecycle.LiveData;

import com.aaronbrecher.neverlate.models.GeofenceModel;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GeofencesRepository {
    GeofencesDao mGeofencesDao;

    @Inject
    public GeofencesRepository(GeofencesDao geofencesDao) {
        mGeofencesDao = geofencesDao;
    }

    public void insertAll(List<GeofenceModel> geofences){
        mGeofencesDao.insertAll(geofences);
    }

    public void insertFence(GeofenceModel fence){
        mGeofencesDao.insertFence(fence);
    }

    public LiveData<GeofenceModel> getGeofencebyKey(String key){
        return mGeofencesDao.getGeofenceByKey(key);
    }

    public void deleteGeofenceWithKey(String key){
        mGeofencesDao.deleteGeofenceWithKey(key);
    }
}
