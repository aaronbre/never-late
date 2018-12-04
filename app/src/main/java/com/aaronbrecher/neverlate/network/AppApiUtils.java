package com.aaronbrecher.neverlate.network;

public class AppApiUtils {
    public static final String DIRECTION_MATRIX_API_ENDPOINT = "directionmatrix";
    static final String DISTANCE_API_ENDPOINT = "distancematrix";
    static final String VERSION_ENDPOINT = "current-version";
    static final String DIRECTION_API_ENDPOINT = "directions";
    private static final String BASE_URL = "https://never-late-api.herokuapp.com/";

    public static AppApiService createService(){
        return RetrofitClient.getClient(BASE_URL).create(AppApiService.class);
    }
}
