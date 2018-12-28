package com.aaronbrecher.neverlate.models.retrofitmodels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Version {

    @SerializedName("version")
    @Expose
    var version: Int = 0

    @SerializedName("message")
    @Expose
    var message: String? = null

    @SerializedName("needsUpdate")
    @Expose
    var needsUpdate: Boolean? = null
}
