package com.aaronbrecher.neverlate.network;

import com.aaronbrecher.neverlate.models.EventLocationDetails;
import com.aaronbrecher.neverlate.models.retrofitmodels.EventDistanceDuration;
import com.aaronbrecher.neverlate.models.retrofitmodels.Version;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AppApiService {

    @POST(AppApiUtils.DIRECTION_MATRIX_API_ENDPOINT)
    Call<List<EventDistanceDuration>> queryHereMatrix(@Query("origin") String origin, @Body List<EventLocationDetails> destinations);

    @POST(AppApiUtils.DIRECTION_API_ENDPOINT)
    Call<EventDistanceDuration> queryDirections(@Query("origin")String origin, @Body EventLocationDetails destination);

    @POST(AppApiUtils.PUBLIC_TRANSIT_API_ENDPOINT)
    Call<List<EventDistanceDuration>> queryHerePublicTransit(@Query("origin") String origin, @Body List<EventLocationDetails> destinations);

    @GET(AppApiUtils.VERSION_ENDPOINT)
    Call<Version> queryVersionNumber(@Query("usersversion")int version);

    @GET(AppApiUtils.VERIFY_PURCHASE_ENDPOINT)
    Call<Boolean> verifyPurchase(@Query("token") String token, @Query("productId") String productId, @Query("packageName") String packageName);
}
