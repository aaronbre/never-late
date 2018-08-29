package com.aaronbrecher.neverlate.models;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.threeten.bp.LocalDateTime;

/**
 * Event class to represent the calendar event in a local DB
 * the id will always equal the same id as the calendar event to
 * allow easy updating and referencing
 */
@Entity(tableName = "events")
public class Event implements Parcelable {

    @PrimaryKey(autoGenerate = false)
    @NonNull
    private int id;

    @ColumnInfo
    @NonNull
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
    private String location;

    @ColumnInfo
    private boolean watching;

    @ColumnInfo
    @Nullable
    private Long distance;

    @ColumnInfo
    @Nullable
    private Long timeTo;

    @Ignore
    public Event(@NonNull int id, @NonNull long calendarId, String title, String description,
                 LocalDateTime startTime, LocalDateTime endTime, String location, boolean watching) {
        this.id = id;
        this.calendarId = calendarId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.watching = watching;
    }

    public Event() {
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
        return description;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isWatching() {
        return watching;
    }

    public void setWatching(boolean watching) {
        this.watching = watching;
    }

    @Nullable
    public Long getDistance() {
        return distance;
    }

    public void setDistance(long distance) {
        this.distance = distance;
    }

    @Nullable
    public Long getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(long timeTo) {
        this.timeTo = timeTo;
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
        dest.writeString(this.location);
    }

    @Ignore
    protected Event(Parcel in) {
        this.id = in.readInt();
        this.calendarId = in.readLong();
        this.title = in.readString();
        this.description = in.readString();
        this.startTime = (LocalDateTime) in.readSerializable();
        this.endTime = (LocalDateTime) in.readSerializable();
        this.location = in.readString();
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        else if (obj == null) return false;
        else if (obj instanceof Event) {
            Event event = (Event) obj;
            if (event.getId() == this.getId()
                    && event.getLocation().equals(this.getLocation())
                    && event.getStartTime() == this.getStartTime()
                    && event.getEndTime() == this.getEndTime()
                    && event.getTitle().equals(this.getTitle())
                    && event.getDescription().equals(this.getDescription())
                    && event.getCalendarId() == this.getCalendarId()
                    && event.watching == this.watching
                    ) return true;
        }
        return false;
    }
}
