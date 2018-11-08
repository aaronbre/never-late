package com.aaronbrecher.neverlate.models

class EventCompatiblity {
    var startEvent: Event? = null
    var endEvent: Event? = null
    var isWithinDrivingDistance: Boolean = false
    var maxTimeAtStartEvent: Int = 0
    var isCanReturnHome: Boolean = false
    var isCanReturnToWork: Boolean = false

    constructor() {}

    constructor(startEvent: Event, endEvent: Event, withinDrivingDistance: Boolean,
                maxTimeAtStartEvent: Int, canReturnHome: Boolean, canReturnToWork: Boolean) {
        this.startEvent = startEvent
        this.endEvent = endEvent
        this.isWithinDrivingDistance = withinDrivingDistance
        this.maxTimeAtStartEvent = maxTimeAtStartEvent
        this.isCanReturnHome = canReturnHome
        this.isCanReturnToWork = canReturnToWork
    }
}
