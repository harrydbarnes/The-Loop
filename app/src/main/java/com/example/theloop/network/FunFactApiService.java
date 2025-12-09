package com.example.theloop.network;

import com.example.theloop.models.FunFactResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface FunFactApiService {
    @GET("random.json?language=en")
    Call<FunFactResponse> getRandomFact();
}
