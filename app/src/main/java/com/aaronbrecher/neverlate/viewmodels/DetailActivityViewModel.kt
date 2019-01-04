package com.aaronbrecher.neverlate.viewmodels

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.AwarenessFencesCreator
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.models.Event
import java.util.*
import javax.inject.Inject

class DetailActivityViewModel @Inject
constructor(eventsRepository: EventsRepository, appExecutors: AppExecutors) : BaseViewModel(eventsRepository, null, appExecutors) {

    fun updateEvent(event: Event) {
        mAppExecutors.diskIO().execute { mEventsRepository.updateEvents(event) }
    }

    fun resetFenceForEventForTransportChange(event: Event) {
        val events = listOf(event)
        val creator = AwarenessFencesCreator.Builder(events).build()
        creator.buildAndSaveFences(true)
    }

    fun removeGeofenceForEvent(event: Event) {
        val creator = AwarenessFencesCreator.Builder(null).build()
        creator.removeFences(event)
    }

}
