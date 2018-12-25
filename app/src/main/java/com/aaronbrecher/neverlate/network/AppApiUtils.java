package com.aaronbrecher.neverlate.network;

public class AppApiUtils {
    static final String DIRECTION_MATRIX_API_ENDPOINT = "direction-matrix";
    static final String VERSION_ENDPOINT = "current-version";
    static final String DIRECTION_API_ENDPOINT = "directions";
    static final String PUBLIC_TRANSIT_API_ENDPOINT = "public-transit";
    static final String VERIFY_PURCHASE_ENDPOINT = "verify-purchase";
    private static final String BASE_URL = "https://never-late-spring-api-staging.herokuapp.com/";

    public static AppApiService createService(){
        return RetrofitClient.getClient(BASE_URL).create(AppApiService.class);
    }
}
