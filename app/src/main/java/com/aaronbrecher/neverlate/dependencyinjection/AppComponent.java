package com.aaronbrecher.neverlate.dependencyinjection;

import com.aaronbrecher.neverlate.backgroundservices.AnaylizeEventsJobService;
import com.aaronbrecher.neverlate.backgroundservices.CheckForCalendarChangedService;
import com.aaronbrecher.neverlate.backgroundservices.broadcastreceivers.DrivingLocationHelper;
import com.aaronbrecher.neverlate.backgroundservices.jobintentservices.ActivityTransitionService;
import com.aaronbrecher.neverlate.backgroundservices.jobintentservices.AwarenessFenceTransitionService;
import com.aaronbrecher.neverlate.geofencing.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.aaronbrecher.neverlate.ui.fragments.CompatibilityFragment;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailMapFragment;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;
import com.aaronbrecher.neverlate.ui.fragments.SnoozeFragment;
import com.aaronbrecher.neverlate.ui.widget.NeverLateWidget;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, RoomModule.class})
public interface AppComponent {
    void inject(MainActivity mainActivity);
    void inject(EventListFragment fragment);
    void inject(EventDetailActivity detailActivity);
    void inject(EventDetailFragment fragment);
    void inject(EventDetailMapFragment fragment);
    void inject(AwarenessFenceTransitionService service);
    void inject(ActivityTransitionService service);
    void inject(NeverLateWidget widget);
    void inject(CheckForCalendarChangedService service);
    void inject(AnaylizeEventsJobService service);
    void inject(DrivingLocationHelper helper);
    void inject(AwarenessFencesCreator fencesCreator);
    void inject(CompatibilityFragment fragment);
    void inject(SnoozeFragment fragment);
}
