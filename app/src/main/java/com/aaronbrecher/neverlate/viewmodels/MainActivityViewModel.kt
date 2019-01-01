package com.aaronbrecher.neverlate.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Utils.BackgroundUtils
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.models.Event
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver

import java.util.ArrayList

import javax.inject.Inject

class MainActivityViewModel @Inject
constructor(eventsRepository: EventsRepository, application: Application, appExecutors: AppExecutors) : BaseViewModel(eventsRepository, application, appExecutors) {
    val event = MutableLiveData<Event>()
    val shouldShowAllEvents = MutableLiveData<Boolean>()
    //this field is to compare previous location so as not to do
    //additional api call on orientation change
    private val mPreviousLocationList = ArrayList<Event>()

    //this field differs from the getAllCurrentEvents as the db is not location aware
    //this field will contain the location info as well (distance and time to travel)
    private val mEventsWithLocation: MutableLiveData<List<Event>>? = null

    val allCurrentEvents: LiveData<List<Event>>
        get() = mEventsRepository.queryAllCurrentTrackedEvents()

    val allEvents: LiveData<List<Event>>
        get() = mEventsRepository.queryEventsNoLocation()

    //insert the events async using a simple async task
    fun insertEvents(events: List<Event>) {
        mAppExecutors.diskIO().execute { mEventsRepository.insertAll(events) }
    }

    override fun setEvent(event: Event) {
        this.event.postValue(event)
    }

    fun setShouldShowAllEvents(bool: Boolean) {
        shouldShowAllEvents.postValue(bool)
    }

    fun updateEvent(event: Event) {
        mAppExecutors.diskIO().execute { mEventsRepository.updateEvents(event) }
    }

    fun setShowAllEvents() {

    }

    fun deleteAllEvents() {
        mEventsRepository.deleteAllEvents()
    }

    fun setSnoozeForTime(endTime: Long) {
        mAppExecutors.diskIO().execute { deleteAllEvents() }
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(mApplication))
        dispatcher.cancelAll()
        dispatcher.mustSchedule(BackgroundUtils.endSnoozeJob(dispatcher, endTime))
    }

    fun rescheduleAllJobs() {
        val jobDispatcher = FirebaseJobDispatcher(GooglePlayDriver(mApplication))
        jobDispatcher.mustSchedule(BackgroundUtils.setUpPeriodicCalendarChecks(jobDispatcher))
        jobDispatcher.mustSchedule(BackgroundUtils.setUpActivityRecognitionJob(jobDispatcher))
    }
}
