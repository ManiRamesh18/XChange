package com.example.data.dao

import androidx.room.*
import com.example.data.entity.ConversionLog
import com.example.data.entity.CurrencyFavorite
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<ConversionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(log: ConversionLog)

    @Delete
    suspend fun deleteHistory(log: ConversionLog)

    @Query("DELETE FROM conversion_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM currency_favorites")
    fun getFavorites(): Flow<List<CurrencyFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: CurrencyFavorite)

    @Delete
    suspend fun removeFavorite(favorite: CurrencyFavorite)

    @Query("DELETE FROM currency_favorites WHERE currencyCode = :currencyCode")
    suspend fun removeFavoriteByCode(currencyCode: String)
}
