package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromCurrency: String,
    val toCurrency: String,
    val fromAmount: Double,
    val toAmount: Double,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "currency_favorites")
data class CurrencyFavorite(
    @PrimaryKey val currencyCode: String,
    val isFavorite: Boolean = true
)
