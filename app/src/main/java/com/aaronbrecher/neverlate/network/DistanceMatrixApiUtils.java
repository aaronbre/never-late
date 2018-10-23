package com.aaronbrecher.neverlate.network;

import retrofit2.Retrofit;

public class DistanceMatrixApiUtils {
    static final String DISTANCE_API_ENDPOINT = "distancematrix";
    private static final String BASE_URL = "https://never-late-api.herokuapp.com/";

    public static DistanceMatrixService createService(){
        return RetrofitClient.getClient(BASE_URL).create(DistanceMatrixService.class);
    }
}
