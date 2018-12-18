package com.aaronbrecher.neverlate.models;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aaronbrecher.neverlate.Constants;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.threeten.bp.LocalDateTime;

import java.util.Comparator;
import java.util.TimeZone;

/**
 * Event class to represent the calendar event in a local DB
 * the id will always equal the same id as the calendar event to
 * allow easy updating and referencing
 */
@Entity(tableName = "events")
public class Event implements Parcelable {

    @PrimaryKey(autoGenerate = false)
    private int id;

    @ColumnInfo
    private long calendarId;

    @ColumnInfo
    private String title;

    @ColumnInfo
    private String description;

    @ColumnInfo
    private LocalDateTime startTime;

    @ColumnInfo
    private LocalDateTime endTime;

    @ColumnInfo
    private LatLng locationLatlng;

    @ColumnInfo
    private String location;

    @ColumnInfo
    private boolean watching;

    @ColumnInfo
    private Long distance;

    @ColumnInfo
    private Long drivingTime;

    @ColumnInfo
    private String origin;

    @ColumnInfo
    private Integer transportMode;

    @Ignore
    public Event(@NonNull int id, @NonNull long calendarId, String title, String description,
                 LocalDateTime startTime, LocalDateTime endTime, String location, LatLng locationLatlng, boolean watching, int transportMode) {
        this.id = id;
        this.calendarId = calendarId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.locationLatlng = locationLatlng;
        this.watching = watching;
        this.transportMode = transportMode;
    }

    public Event() {
    }

    @Ignore
    public Event copy() {
        Event event = new Event(this.getId(), this.getCalendarId(), this.getTitle(), this.getDescription(),
                this.getStartTime(), this.getEndTime(), this.getLocation(), this.getLocationLatlng(), this.isWatching(), this.getTransportMode());
        event.setDistance(this.getDistance());
        event.setDrivingTime(this.getDrivingTime());
        return event;
    }

    @NonNull
    public int getId() {
        return id;
    }

    public void setId(@NonNull int id) {
        this.id = id;
    }

    @NonNull
    public long getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(@NonNull long calendarId) {
        this.calendarId = calendarId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return this.location;
    }

    public LatLng getLocationLatlng() {
        return locationLatlng;
    }

    public void setLocationLatlng(LatLng location) {
        this.locationLatlng = location;
    }

    public boolean isWatching() {
        return watching;
    }

    public void setWatching(boolean watching) {
        this.watching = watching;
    }

    public Long getDistance() {
        return distance;
    }

    public void setDistance(Long distance) {
        this.distance = distance;
    }

    public Long getDrivingTime() {
        return drivingTime;
    }

    public void setDrivingTime(Long drivingTime) {
        this.drivingTime = drivingTime;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Integer getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(Integer transportMode) {
        this.transportMode = transportMode;
    }

    @Ignore
    @Override
    public int describeContents() {
        return 0;
    }

    @Ignore
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeLong(this.calendarId);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeSerializable(this.startTime);
        dest.writeSerializable(this.endTime);
        dest.writeParcelable(this.locationLatlng, flags);
        dest.writeString(this.location);
        dest.writeByte(this.watching ? (byte) 1 : (byte) 0);
        dest.writeValue(this.distance);
        dest.writeValue(this.drivingTime);
    }

    @Ignore
    protected Event(Parcel in) {
        this.id = in.readInt();
        this.calendarId = in.readLong();
        this.title = in.readString();
        this.description = in.readString();
        this.startTime = (LocalDateTime) in.readSerializable();
        this.endTime = (LocalDateTime) in.readSerializable();
        this.locationLatlng = in.readParcelable(LatLng.class.getClassLoader());
        this.location = in.readString();
        this.watching = in.readByte() != 0;
        this.distance = (Long) in.readValue(Long.class.getClassLoader());
        this.drivingTime = (Long) in.readValue(Long.class.getClassLoader());
    }

    @Ignore
    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel source) {
            return new Event(source);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    @Ignore
    public static String convertEventToJson(Event event) {
        return new Gson().toJson(event);
    }

    @Ignore
    public static Event convertJsonToEvent(String json) {
        return new Gson().fromJson(json, Event.class);
    }

    /*
     *Comparator function to sort a list of events by the ID rather then time
     */
    @Ignore
    public static Comparator<Event> eventIdComparator = (event1, event2) -> event1.id - event2.id;

    /**
     * shows different possible changes
     * Same indicates no change
     * DESCRIPTION_CHANGE indicates a change in title etc. that will not need to reset fences
     * GEOFENCE_CHANGE indicates a change in time or location and needs to update fences
     */
    public enum Change {
        SAME, DESCRIPTION_CHANGE, GEOFENCE_CHANGE
    }

    /**
     * Determine if there is a change in event as well as what type of change
     *
     * @param oldEvent the event as it is saved in DB
     * @param newEvent the event as it is currently in the calendar
     * @return a Change enum signifying what type of change occured
     */
    @Ignore
    public static Change eventChanged(Event oldEvent, Event newEvent) {
        if (oldEvent == null && newEvent != null) {
            return Change.GEOFENCE_CHANGE;
        } else if ((oldEvent != null && newEvent == null) || oldEvent == null) {
            return Change.SAME;
        }
        if (!oldEvent.getStartTime().equals(newEvent.getStartTime())
                || !oldEvent.getEndTime().equals(newEvent.getEndTime())
                || !oldEvent.getLocation().equals(newEvent.getLocation())) return Change.GEOFENCE_CHANGE;

        //TODO remove null check now that description will always be at least an empty string
        if (!oldEvent.getTitle().equals(newEvent.getTitle())
                || !oldEvent.getDescription().equals(newEvent.getDescription()))
            return Change.DESCRIPTION_CHANGE;

        return Change.SAME;
    }
}
