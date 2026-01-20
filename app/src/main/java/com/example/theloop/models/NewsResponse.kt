package com.example.theloop.models

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    @SerializedName("Business") val business: List<Article>?,
    @SerializedName("Entertainment") val entertainment: List<Article>?,
    @SerializedName("Health") val health: List<Article>?,
    @SerializedName("Science") val science: List<Article>?,
    @SerializedName("Sports") val sports: List<Article>?,
    @SerializedName("Technology") val technology: List<Article>?,
    @SerializedName("US") val us: List<Article>?,
    @SerializedName("World") val world: List<Article>?
)
