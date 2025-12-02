package com.example.theloop.models;

import com.google.gson.annotations.SerializedName;

public class Article {

    @SerializedName("source")
    private String source;

    @SerializedName("title")
    private String title;

    @SerializedName("link")
    private String url;

    // Getters
    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
