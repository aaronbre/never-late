package com.aaronbrecher.neverlate.network

import com.aaronbrecher.neverlate.models.EventLocationDetails
import com.aaronbrecher.neverlate.models.retrofitmodels.EventDistanceDuration
import com.aaronbrecher.neverlate.models.retrofitmodels.Version

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AppApiService {

    @POST(DIRECTION_MATRIX_API_ENDPOINT)
    fun queryHereMatrix(@Query("origin") origin: String, @Body destinations: List<EventLocationDetails>): Call<List<EventDistanceDuration>>

    @POST(DIRECTION_API_ENDPOINT)
    fun queryDirections(@Query("origin") origin: String, @Body destination: EventLocationDetails): Call<EventDistanceDuration>

    @POST(PUBLIC_TRANSIT_API_ENDPOINT)
    fun queryHerePublicTransit(@Query("origin") origin: String, @Body destinations: List<EventLocationDetails>): Call<List<EventDistanceDuration>>

    @GET(VERSION_ENDPOINT)
    fun queryVersionNumber(@Query("usersversion") version: Int): Call<Version>

    @GET(VERIFY_PURCHASE_ENDPOINT)
    fun verifyPurchase(@Query("token") token: String, @Query("productId") productId: String, @Query("packageName") packageName: String): Call<Boolean>
}
