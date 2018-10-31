package com.aaronbrecher.neverlate.network;

import com.aaronbrecher.neverlate.models.retrofitmodels.DistanceMatrix;
import com.aaronbrecher.neverlate.models.retrofitmodels.Version;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AppApiService {
    @GET(AppApiUtils.DISTANCE_API_ENDPOINT)
    Call<DistanceMatrix> queryDistanceMatrix(@Query("origin")String origin, @Query("destinations")String destinations);

    @GET(AppApiUtils.VERSION_ENDPOINT)
    Call<Version> queryVersionNumber();
}
