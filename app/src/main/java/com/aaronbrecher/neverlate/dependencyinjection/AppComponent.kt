package com.aaronbrecher.neverlate.dependencyinjection

import com.aaronbrecher.neverlate.backgroundservices.AnaylizeEventsJobService
import com.aaronbrecher.neverlate.backgroundservices.CheckForCalendarChangedService
import com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers.DrivingLocationHelper
import com.aaronbrecher.neverlate.backgroundservices.jobintentservices.ActivityTransitionService
import com.aaronbrecher.neverlate.backgroundservices.jobintentservices.AwarenessFenceTransitionService
import com.aaronbrecher.neverlate.AwarenessFencesCreator
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity
import com.aaronbrecher.neverlate.ui.activities.MainActivity
import com.aaronbrecher.neverlate.ui.fragments.ConflictAnalysisFragment
import com.aaronbrecher.neverlate.ui.fragments.ConflictEmptyFragment
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment
import com.aaronbrecher.neverlate.ui.fragments.EventDetailMapFragment
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment
import com.aaronbrecher.neverlate.ui.fragments.SettingsFragment
import com.aaronbrecher.neverlate.ui.fragments.SnoozeFragment
import com.aaronbrecher.neverlate.ui.widget.NeverLateWidget

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = [AppModule::class, RoomModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(fragment: EventListFragment)
    fun inject(detailActivity: EventDetailActivity)
    fun inject(fragment: EventDetailFragment)
    fun inject(fragment: EventDetailMapFragment)
    fun inject(service: AwarenessFenceTransitionService)
    fun inject(service: ActivityTransitionService)
    fun inject(widget: NeverLateWidget)
    fun inject(service: CheckForCalendarChangedService)
    fun inject(service: AnaylizeEventsJobService)
    fun inject(helper: DrivingLocationHelper)
    fun inject(fencesCreator: AwarenessFencesCreator)
    fun inject(fragment: ConflictAnalysisFragment)
    fun inject(fragment: SnoozeFragment)
    fun inject(fragment: ConflictEmptyFragment)
    fun inject(fragment: SettingsFragment)
}
