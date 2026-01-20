package com.example.theloop.models

import com.google.gson.annotations.SerializedName

data class Article(
    @SerializedName("source") val source: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("link") val url: String?
)
