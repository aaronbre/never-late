package com.aaronbrecher.neverlate.models

data class EventLocationDetails(var latitude: String = "",
                                var longitude: String = "",
                                var arrivalTime: String= ""){
    constructor(latitude: String,longitude: String) : this(latitude,longitude, "")
}