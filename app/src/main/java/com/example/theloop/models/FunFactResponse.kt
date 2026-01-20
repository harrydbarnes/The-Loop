package com.example.theloop.models

import com.google.gson.annotations.SerializedName

data class FunFactResponse(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("source") val source: String
)
