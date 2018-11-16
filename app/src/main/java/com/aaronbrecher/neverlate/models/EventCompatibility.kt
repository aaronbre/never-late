package com.aaronbrecher.neverlate.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "compatibility")
data class EventCompatibility(
        @PrimaryKey(autoGenerate = true) var id: Int?,
        @ColumnInfo(name = "start_event") var startEvent: Int?,
        @ColumnInfo(name = "end_event") var endEvent: Int?,
        @ColumnInfo(name = "compatibility_type") var withinDrivingDistance: Compatible,
        @ColumnInfo(name = "max_time_to_stay") var maxTimeAtStartEvent: Long,
        @ColumnInfo(name = "can_return_home") var canReturnHome: Boolean,
        @ColumnInfo(name = "can_return_to_work") var canReturnToWork: Boolean
) {
    constructor() : this(null, null, null, Compatible.UNKNOWN, 0, false, false)

    enum class Compatible {
        TRUE, FALSE, UNKNOWN
    }
}
