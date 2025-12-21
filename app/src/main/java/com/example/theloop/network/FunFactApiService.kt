package com.example.theloop.network

import com.example.theloop.models.FunFactResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FunFactApiService {
    @GET("random.json")
    suspend fun getRandomFact(@Query("language") language: String): Response<FunFactResponse>
}
