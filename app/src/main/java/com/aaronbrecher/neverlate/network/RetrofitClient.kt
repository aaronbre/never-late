package com.aaronbrecher.neverlate.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


private var retrofit: Retrofit? = null

fun getRetrofitClient(baseUrl: String): Retrofit {
    if (retrofit == null) {
        val httpClient = OkHttpClient.Builder()
        retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()
    }
    return retrofit!!
}

