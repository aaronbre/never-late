package com.aaronbrecher.neverlate.models

import android.os.Parcel
import android.os.Parcelable

import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson

import org.threeten.bp.LocalDateTime

import java.util.Comparator

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Event class to represent the calendar event in a local DB
 * the id will always equal the same id as the calendar event to
 * allow easy updating and referencing
 */
@Entity(tableName = "events")
class Event {

    @PrimaryKey(autoGenerate = false)
    @get:NonNull
    var id: Int = 0

    @ColumnInfo
    @get:NonNull
    var calendarId: Long = 0

    @ColumnInfo
    var title: String? = null

    @ColumnInfo
    var description: String? = null
        get() = if (field != null) field else ""

    @ColumnInfo
    var startTime: LocalDateTime? = null

    @ColumnInfo
    var endTime: LocalDateTime? = null

    @ColumnInfo
    var locationLatlng: LatLng? = null

    @ColumnInfo
    var location: String? = null

    @ColumnInfo
    var watching: Boolean? = false

    @ColumnInfo
    var distance: Long? = null

    @ColumnInfo
    var drivingTime: Long? = null

    @ColumnInfo
    var origin: String? = null

    @ColumnInfo
    var transportMode: Int? = null

    @Ignore
    constructor(@NonNull id: Int, @NonNull calendarId: Long, title: String?, description: String?,
                startTime: LocalDateTime?, endTime: LocalDateTime?, location: String?, locationLatlng: LatLng?, watching: Boolean?, transportMode: Int?) {
        this.id = id
        this.calendarId = calendarId
        this.title = title
        this.description = description
        this.startTime = startTime
        this.endTime = endTime
        this.location = location
        this.locationLatlng = locationLatlng
        this.watching = watching
        this.transportMode = transportMode
    }

    constructor() {}

    @Ignore
    fun copy(): Event {
        val event = Event(this.id, this.calendarId, this.title, this.description,
                this.startTime, this.endTime, this.location, this.locationLatlng, this.watching, this.transportMode!!)
        event.distance = this.distance
        event.drivingTime = this.drivingTime
        return event
    }

    /**
     * shows different possible changes
     * Same indicates no change
     * DESCRIPTION_CHANGE indicates a change in title etc. that will not need to reset fences
     * GEOFENCE_CHANGE indicates a change in time or location and needs to update fences
     */
    enum class Change {
        SAME, DESCRIPTION_CHANGE, GEOFENCE_CHANGE
    }

    companion object {


        fun convertEventToJson(event: Event): String {
            return Gson().toJson(event)
        }

        fun convertJsonToEvent(json: String): Event {
            return Gson().fromJson(json, Event::class.java)
        }

        /*
     *Comparator function to sort a list of events by the ID rather then time
     */
        var eventIdComparator = { event1: Event, event2: Event -> event1.id - event2.id }

        /**
         * Determine if there is a change in event as well as what type of change
         *
         * @param oldEvent the event as it is saved in DB
         * @param newEvent the event as it is currently in the calendar
         * @return a Change enum signifying what type of change occured
         */
        fun eventChanged(oldEvent: Event?, newEvent: Event?): Change {
            if (oldEvent == null && newEvent != null) {
                return Change.GEOFENCE_CHANGE
            } else if (oldEvent != null && newEvent == null || oldEvent == null) {
                return Change.SAME
            }
            if (oldEvent.startTime != newEvent!!.startTime
                    || oldEvent.endTime != newEvent.endTime
                    || oldEvent.location != newEvent.location)
                return Change.GEOFENCE_CHANGE

            //TODO remove null check now that description will always be at least an empty string
            return if (oldEvent.title != newEvent.title || oldEvent.description != newEvent.description) Change.DESCRIPTION_CHANGE else Change.SAME

        }
    }
}