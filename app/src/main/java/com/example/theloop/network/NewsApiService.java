package com.example.theloop.network;

import com.example.theloop.models.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface NewsApiService {

    @GET("news-feed")
    Call<NewsResponse> getNewsFeed();
}
