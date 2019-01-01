package com.aaronbrecher.neverlate.database

import androidx.lifecycle.LiveData

import com.aaronbrecher.neverlate.models.EventCompatibility

import javax.inject.Inject

class EventCompatibilityRepository @Inject
constructor(private var mDao: EventCompatibilityDao) {

    fun insertAll(list: List<EventCompatibility>) {
        mDao.insertAll(list)
    }

    fun queryCompatibility(): LiveData<List<EventCompatibility>?> {
        return mDao.allCompatibilities
    }

    fun deleteAll() {
        mDao.deleteAllEvents()
    }
}
