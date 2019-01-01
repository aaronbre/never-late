package com.aaronbrecher.neverlate.models

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

@Entity(tableName = "compatibility")
data class EventCompatibility(
        @PrimaryKey(autoGenerate = true) var id: Int? = null,
        @ColumnInfo(name = "start_event") var startEvent: Int? = null,
        @ColumnInfo(name = "end_event") var endEvent: Int? = null,
        @ColumnInfo(name = "compatibility_type") var withinDrivingDistance: Compatible = Compatible.UNKNOWN,
        @ColumnInfo(name = "max_time_to_stay") var maxTimeAtStartEvent: Long? = 0,
        @ColumnInfo(name = "can_return_home") var canReturnHome: Boolean? = false,
        @ColumnInfo(name = "can_return_to_work") var canReturnToWork: Boolean? = false
) {

    enum class Compatible {
        TRUE, FALSE, UNKNOWN
    }
}
