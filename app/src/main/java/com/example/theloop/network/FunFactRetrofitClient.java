package com.example.theloop.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FunFactRetrofitClient {
    private static volatile Retrofit retrofit = null;
    private static final String BASE_URL = "https://uselessfacts.jsph.pl/";

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (FunFactRetrofitClient.class) {
                if (retrofit == null) {
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }
}
