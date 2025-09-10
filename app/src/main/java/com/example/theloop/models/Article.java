package com.example.theloop.models;

import com.google.gson.annotations.SerializedName;

public class Article {

    @SerializedName("source")
    private Source source;

    @SerializedName("title")
    private String title;

    @SerializedName("url")
    private String url;

    @SerializedName("publishedAt")
    private String publishedAt;

    // Getters
    public Source getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getPublishedAt() {
        return publishedAt;
    }
}
