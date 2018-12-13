package com.aaronbrecher.neverlate.network;

import com.aaronbrecher.neverlate.models.EventLocationDetails;
import com.aaronbrecher.neverlate.models.retrofitmodels.DirectionsDuration;
import com.aaronbrecher.neverlate.models.retrofitmodels.EventDistanceDuration;
import com.aaronbrecher.neverlate.models.retrofitmodels.googleDistanceMatrix.DistanceMatrix;
import com.aaronbrecher.neverlate.models.retrofitmodels.MapboxDirectionMatrix.MapboxDirectionMatrix;
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

    @GET(AppApiUtils.VERSION_ENDPOINT)
    Call<Version> queryVersionNumber(@Query("usersversion")int version);
}
