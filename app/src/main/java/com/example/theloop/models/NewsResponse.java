package com.example.theloop.models;

import java.util.List;
import java.util.Map;
import com.google.gson.annotations.SerializedName;

public class NewsResponse {

    @SerializedName("Business")
    private List<Article> business;

    @SerializedName("Entertainment")
    private List<Article> entertainment;

    @SerializedName("Health")
    private List<Article> health;

    @SerializedName("Science")
    private List<Article> science;

    @SerializedName("Sports")
    private List<Article> sports;

    @SerializedName("Technology")
    private List<Article> technology;

    @SerializedName("US")
    private List<Article> us;

    @SerializedName("World")
    private List<Article> world;

    public List<Article> getBusiness() { return business; }
    public List<Article> getEntertainment() { return entertainment; }
    public List<Article> getHealth() { return health; }
    public List<Article> getScience() { return science; }
    public List<Article> getSports() { return sports; }
    public List<Article> getTechnology() { return technology; }
    public List<Article> getUs() { return us; }
    public List<Article> getWorld() { return world; }
}
