package com.aaronbrecher.neverlate.models;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

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
    private long startTime;

    @ColumnInfo
    private long endTime;

    @ColumnInfo
    private String location;

    @Ignore
    public Event(@NonNull int id, @NonNull long calendarId, String title, String description, long startTime, long endTime, String location) {
        this.id = id;
        this.calendarId = calendarId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    @Ignore
    public int describeContents() {
        return 0;
    }

    @Override
    @Ignore
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeLong(this.calendarId);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeLong(this.startTime);
        dest.writeLong(this.endTime);
        dest.writeString(this.location);
    }

    @Ignore
    protected Event(Parcel in) {
        this.id = in.readInt();
        this.calendarId = in.readLong();
        this.title = in.readString();
        this.description = in.readString();
        this.startTime = in.readLong();
        this.endTime = in.readLong();
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
}
