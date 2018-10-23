package com.aaronbrecher.neverlate.network;

import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.retrofitmodels.DistanceMatrix;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DistanceMatrixService {
    @GET(DistanceMatrixApiUtils.DISTANCE_API_ENDPOINT)
    Call<DistanceMatrix> queryDistanceMatrix(@Query("origin")String origin, @Query("destinations")String destinations);
}
