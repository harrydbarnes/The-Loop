package com.example.theloop.models;

import com.google.gson.annotations.SerializedName;

public class Source {

    @SerializedName("name")
    private String name;

    // Getter
    public String getName() {
        return name;
    }
}
