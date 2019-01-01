package com.aaronbrecher.neverlate.viewmodels

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.models.Event

abstract class BaseViewModel internal constructor(protected var mEventsRepository: EventsRepository,
                                                  protected var mApplication: Application?,
                                                  protected var mAppExecutors: AppExecutors) : ViewModel() {

    var mEvent: MutableLiveData<Event> = MutableLiveData()

    open fun setEvent(event: Event) {
        mEvent.postValue(event)
    }
}
