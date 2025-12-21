package com.example.theloop.models;

import com.google.gson.annotations.SerializedName;

public class FunFactResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("text")
    private String text;

    @SerializedName("source")
    private String source;

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getSource() {
        return source;
    }
}
