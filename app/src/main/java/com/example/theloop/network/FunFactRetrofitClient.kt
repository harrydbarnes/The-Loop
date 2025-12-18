package com.example.theloop.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FunFactRetrofitClient {
    private const val BASE_URL = "https://uselessfacts.jsph.pl/"

    val client: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
