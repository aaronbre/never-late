package com.aaronbrecher.neverlate.network;

public class AppApiUtils {
    static final String DISTANCE_API_ENDPOINT = "distancematrix";
    static final String VERSION_ENDPOINT = "current-version";
    private static final String BASE_URL = "https://never-late-api.herokuapp.com/";

    public static AppApiService createService(){
        return RetrofitClient.getClient(BASE_URL).create(AppApiService.class);
    }
}
