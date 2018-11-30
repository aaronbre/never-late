package com.aaronbrecher.neverlate.backgroundservices;

import android.location.Location;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatibility;
import com.aaronbrecher.neverlate.models.retrofitmodels.MapboxDirectionMatrix.MapboxDirectionMatrix;
import com.aaronbrecher.neverlate.network.AppApiService;
import com.aaronbrecher.neverlate.network.AppApiUtils;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;

public class AnaylizeEventsJobService extends JobService {
    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors mAppExecutors;
    @Inject
    EventCompatibilityRepository mCompatabilityRepository;

    private AppApiService mApiService;
    private JobParameters mJobParameters;
    private List<Event> mEventList;
    private List<EventCompatibility> mEventCompatibilities;

    @Override
    public void onCreate() {
        super.onCreate();
        NeverLateApp.getApp().getAppComponent()
                .inject(this);
        mApiService = AppApiUtils.createService();
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        mJobParameters = job;
        mAppExecutors.diskIO().execute(this::doWork);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    private void doWork() {
        mEventCompatibilities = new ArrayList<>();
        mEventList = mEventsRepository.queryAllCurrentTrackedEventsSync();
        if (mEventList == null || mEventList.size() < 2) {
            mAppExecutors.mainThread().execute(() -> MainActivity.setFinishedLoading(true));
            return;
        }
        for (int i = 0; i < mEventList.size() - 1; i++) {
            mEventCompatibilities.add(getCompatibility(mEventList.get(i), mEventList.get(i + 1)));
        }
        //todo figure out a better way of doing this
        mCompatabilityRepository.deleteAll();
        try{
            mCompatabilityRepository.insertAll(mEventCompatibilities);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        mAppExecutors.mainThread().execute(() -> MainActivity.setFinishedLoading(true));
        jobFinished(mJobParameters, false);
    }

    private EventCompatibility getCompatibility(Event event1, Event event2) {
        EventCompatibility eventCompatibility = new EventCompatibility();
        eventCompatibility.setStartEvent(event1.getId());
        eventCompatibility.setEndEvent(event2.getId());

        LatLng originLatLng = event1.getLocationLatlng();
        LatLng destinationLatLng = event2.getLocationLatlng();
        if (originLatLng == null || destinationLatLng == null) return null;
        String origin = originLatLng.longitude + "," + originLatLng.latitude;
        String destination = destinationLatLng.longitude + "," + destinationLatLng.latitude;
        double duration = getMapboxDrivingDuration(origin, destination) * 1000;
        if (duration < 0)
            eventCompatibility.setWithinDrivingDistance(EventCompatibility.Compatible.UNKNOWN);
        else {
            determineComparabilityAndTiming(event1, event2, eventCompatibility, duration);
        }
        return eventCompatibility;
    }

    /**
     * get the mapbox distance
     */
    private double getMapboxDrivingDuration(String origin, String destination) {
        Call<MapboxDirectionMatrix> call = mApiService.queryMapboxDirectionMatrix(origin, destination, 1);
        try {
            MapboxDirectionMatrix matrix = call.execute().body();
            if (matrix == null || matrix.getDurations().size() < 1) return -1;
            return matrix.getDurations().get(0).get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void determineComparabilityAndTiming(Event event1, Event event2, EventCompatibility eventCompatibility, double duration) {
        long firstEventStart = Converters.unixFromDateTime(event1.getStartTime());
        long secondEventStart = Converters.unixFromDateTime(event2.getStartTime());
        long arrivalTimeToSecondEvent = firstEventStart + (long) duration;
        if (arrivalTimeToSecondEvent > secondEventStart) {
            eventCompatibility.setWithinDrivingDistance(EventCompatibility.Compatible.FALSE);
            eventCompatibility.setCanReturnHome(false);
            eventCompatibility.setCanReturnToWork(false);
            eventCompatibility.setMaxTimeAtStartEvent(Constants.ROOM_INVALID_LONG_VALUE);
        } else {
            eventCompatibility.setWithinDrivingDistance(EventCompatibility.Compatible.TRUE);
            long maximumTimeAtEvent = secondEventStart - arrivalTimeToSecondEvent;
            //TODO Setting this to false to avoid bugs in future fix this
            eventCompatibility.setCanReturnHome(false);
            eventCompatibility.setCanReturnToWork(false);
            eventCompatibility.setMaxTimeAtStartEvent(maximumTimeAtEvent);
        }

    }

    /**
     * Gets the "as the crow flies" distance between two events
     *
     * @return the distance in KM
     */
    private float getCrowFlyDistance(LatLng origin, LatLng destination) {
        Location locationorigin = new Location("never-late");
        Location locationDest = new Location("never-late");
        locationorigin.setLatitude(origin.latitude);
        locationorigin.setLongitude(origin.longitude);
        locationDest.setLatitude(destination.latitude);
        locationDest.setLongitude(origin.longitude);
        return locationorigin.distanceTo(locationDest) / 1000;

    }
}
