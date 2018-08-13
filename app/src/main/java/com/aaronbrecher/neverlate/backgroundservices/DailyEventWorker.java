package com.aaronbrecher.neverlate.backgroundservices;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.Utils.CalendarUtils;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.database.GeofencesRepository;
import com.aaronbrecher.neverlate.geofencing.Geofencing;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.work.Worker;

public class DailyEventWorker extends Worker{

    private Context mContext;
    private EventsRepository mEventsRepository;
    private GeofencesRepository mGeofencesRepository;
    private List<Event> mEventList;

    @Inject
    public DailyEventWorker(EventsRepository eventsRepository, GeofencesRepository geofencesRepository) {
        this.mEventsRepository = eventsRepository;
        this.mGeofencesRepository = geofencesRepository;
    }
    @NonNull
    @Override
    public Result doWork() {
        mContext = getApplicationContext();
        mEventList = mEventsRepository.queryAllEventsNotLive();
        deleteOldEvents();
        addCalendarEventsToDb();
        return null;
    }

    /**
     * Delete all events that has passed the end time, it will also delete all
     * the Geofences from the db. In this case there is no need to unregister the
     * Geofence from the locationService as they expired by the endTime...
     */
    private void deleteOldEvents() {
        GeofencingClient geoClient = LocationServices.getGeofencingClient(mContext);
        List<String> oldGeofences = new ArrayList<>();
        List<Event> oldEvents = new ArrayList<>();
        for(Event event : mEventList){
            if(Converters.unixFromDateTime(event.getEndTime()) > System.currentTimeMillis()){
                String geofenceRequestId = Constants.GEOFENCE_REQUEST_ID + event.getId();
                oldGeofences.add(geofenceRequestId);
                oldEvents.add(event);
            }
        }
        mEventsRepository.deleteEvents(oldEvents.toArray(new Event[oldEvents.size()]));
        for(String id : oldGeofences){ mGeofencesRepository.deleteGeofenceWithKey(id); }
    }

    /**
     * Add all calendar events into
     */
    private void addCalendarEventsToDb() {
        List<Event> events = CalendarUtils.getCalendarEventsForToday(mContext);
        mEventsRepository.insertAll(events);
        Geofencing geofencing = new Geofencing(mContext,
                mEventsRepository.queryAllEventsNotLive(),
                PreferenceManager.getDefaultSharedPreferences(mContext));
        List<GeofenceModel> geofenceModels = geofencing.setUpGeofences();
        mGeofencesRepository.insertAll(geofenceModels);
    }
}
