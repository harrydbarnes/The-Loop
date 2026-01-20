package com.example.theloop.data.repository

import com.example.theloop.network.FunFactApiService
import javax.inject.Inject

class FunFactRepository @Inject constructor(
    private val api: FunFactApiService
) {
    suspend fun getFunFact(): String? {
        return try {
            val response = api.getRandomFact("en")
            if (response.isSuccessful) {
                response.body()?.text
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
