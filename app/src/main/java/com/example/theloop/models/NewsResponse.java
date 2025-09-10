package com.example.theloop.models;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class NewsResponse {

    @SerializedName("articles")
    private List<Article> articles;

    public List<Article> getArticles() {
        return articles;
    }
}
