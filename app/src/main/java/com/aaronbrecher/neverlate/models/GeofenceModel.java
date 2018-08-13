package com.aaronbrecher.neverlate.models;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "geofences")
public class GeofenceModel {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;

    @ColumnInfo
    private String requestKey;

    @ColumnInfo
    private int fenceRadius;

    public GeofenceModel() {
    }

    @Ignore
    public GeofenceModel(String requestKey, int fenceRadius) {
        this.requestKey = requestKey;
        this.fenceRadius = fenceRadius;
    }

    @NonNull
    public int getId() {
        return id;
    }

    public void setId(@NonNull int id) {
        this.id = id;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public int getFenceRadius() {
        return fenceRadius;
    }

    public void setFenceRadius(int fenceRadius) {
        this.fenceRadius = fenceRadius;
    }
}
