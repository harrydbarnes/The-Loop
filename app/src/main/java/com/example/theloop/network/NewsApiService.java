package com.example.theloop.network;

import com.example.theloop.models.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NewsApiService {

    @GET("top-headlines/category/{category}/{country}.json")
    Call<NewsResponse> getTopHeadlines(
            @Path("category") String category,
            @Path("country") String country
    );
}
