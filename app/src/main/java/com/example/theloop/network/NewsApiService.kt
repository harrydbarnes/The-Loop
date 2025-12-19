package com.example.theloop.network

import com.example.theloop.models.NewsResponse
import retrofit2.Response
import retrofit2.http.GET

interface NewsApiService {
    @GET("news-feed")
    suspend fun getNewsFeed(): Response<NewsResponse>
}
