package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CurrencyDao
import com.example.data.entity.ConversionLog
import com.example.data.entity.CurrencyFavorite

@Database(entities = [ConversionLog::class, CurrencyFavorite::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "currency_exchange_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
