package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LatestRatesResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
