package com.aaronbrecher.neverlate.network;

import com.aaronbrecher.neverlate.models.retrofitmodels.DistanceMatrix;
import com.aaronbrecher.neverlate.models.retrofitmodels.MapboxDirectionMatrix;
import com.aaronbrecher.neverlate.models.retrofitmodels.Version;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AppApiService {
    @GET(AppApiUtils.DISTANCE_API_ENDPOINT)
    Call<DistanceMatrix> queryDistanceMatrix(@Query("origin")String origin, @Query("destinations")String destinations);

//    mapbox endpoint
    @GET(AppApiUtils.DIRECTION_MATRIX_API_ENDPOINT)
    Call<MapboxDirectionMatrix> queryDirectionMatrix(@Query("origin")String origin, @Query("destinations")String destinations, @Query("destinationsize") int size);

    @GET(AppApiUtils.VERSION_ENDPOINT)
    Call<Version> queryVersionNumber();
}
