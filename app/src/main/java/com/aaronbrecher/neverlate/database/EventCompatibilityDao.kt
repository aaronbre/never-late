package com.aaronbrecher.neverlate.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.aaronbrecher.neverlate.models.EventCompatibility

@Dao
interface EventCompatibilityDao {

    @get:Query("SELECT * FROM compatibility")
    val allCompatibilities: LiveData<List<EventCompatibility>?>

    @Query("DELETE FROM compatibility")
    fun deleteAllEvents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<EventCompatibility>)
}
