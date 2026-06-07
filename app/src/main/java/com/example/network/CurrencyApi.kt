package com.example.network

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") base: String
    ): LatestRatesResponse

    companion object {
        private const val BASE_URL = "https://api.frankfurter.app/"

        fun create(): CurrencyApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(CurrencyApi::class.java)
        }
    }
}
