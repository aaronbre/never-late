package com.aaronbrecher.neverlate.network


const val DIRECTION_MATRIX_API_ENDPOINT = "direction-matrix"
const val VERSION_ENDPOINT = "current-version"
const val DIRECTION_API_ENDPOINT = "directions"
const val PUBLIC_TRANSIT_API_ENDPOINT = "public-transit"
const val VERIFY_PURCHASE_ENDPOINT = "verify-purchase"
private const val BASE_URL = "https://never-late-spring-api-staging.herokuapp.com/"

fun createRetrofitService(): AppApiService {
    return getRetrofitClient(BASE_URL).create(AppApiService::class.java)
}
