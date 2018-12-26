package com.aaronbrecher.neverlate.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.annotation.NonNull

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository
import com.aaronbrecher.neverlate.database.EventsRepository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomViewModelFactory @Inject
constructor(private val mEventsRepository: EventsRepository, compatabilityRepository: EventCompatibilityRepository,
            private val mApplication: Application, private val mAppExecutors: AppExecutors) : ViewModelProvider.Factory {

    @NonNull
    override fun <T : ViewModel> create(@NonNull modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainActivityViewModel::class.java) -> MainActivityViewModel(mEventsRepository, mApplication, mAppExecutors) as T
            modelClass.isAssignableFrom(DetailActivityViewModel::class.java) -> DetailActivityViewModel(mEventsRepository, mAppExecutors) as T
            else -> throw IllegalArgumentException("ViewModel does not exist")
        }
    }


}
