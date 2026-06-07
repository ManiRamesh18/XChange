package com.example.data.repository

import android.util.Log
import com.example.data.dao.CurrencyDao
import com.example.data.entity.ConversionLog
import com.example.data.entity.CurrencyFavorite
import com.example.network.CurrencyApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class CurrencyInfo(
    val code: String,
    val name: String,
    val flag: String,
    val symbol: String
)

class CurrencyRepository(
    private val currencyDao: CurrencyDao,
    private val currencyApi: CurrencyApi
) {
    // 20 Major Currencies
    val supportedCurrencies = listOf(
        CurrencyInfo("USD", "US Dollar", "🇺🇸", "$"),
        CurrencyInfo("EUR", "Euro", "🇪🇺", "€"),
        CurrencyInfo("GBP", "British Pound", "🇬🇧", "£"),
        CurrencyInfo("JPY", "Japanese Yen", "🇯🇵", "¥"),
        CurrencyInfo("AUD", "Australian Dollar", "🇦🇺", "A$"),
        CurrencyInfo("CAD", "Canadian Dollar", "🇨🇦", "C$"),
        CurrencyInfo("CHF", "Swiss Franc", "🇨🇭", "CHF"),
        CurrencyInfo("CNY", "Chinese Yuan", "🇨🇳", "¥"),
        CurrencyInfo("INR", "Indian Rupee", "🇮🇳", "₹"),
        CurrencyInfo("SGD", "Singapore Dollar", "🇸🇬", "S$"),
        CurrencyInfo("HKD", "Hong Kong Dollar", "🇭🇰", "HK$"),
        CurrencyInfo("NZD", "New Zealand Dollar", "🇳🇿", "NZ$"),
        CurrencyInfo("MXN", "Mexican Peso", "🇲🇽", "MX$"),
        CurrencyInfo("BRL", "Brazilian Real", "🇧🇷", "R$"),
        CurrencyInfo("ZAR", "South African Rand", "🇿🇦", "R"),
        CurrencyInfo("AED", "UAE Dirham", "🇦🇪", "AED"),
        CurrencyInfo("SEK", "Swedish Krona", "🇸🇪", "kr"),
        CurrencyInfo("NOK", "Norwegian Krone", "🇳🇴", "kr"),
        CurrencyInfo("TRY", "Turkish Lira", "🇹🇷", "₺"),
        CurrencyInfo("KRW", "South Korean Won", "🇰🇷", "₩")
    )

    // Baseline offline conversion rates relative to USD (1 USD = X currency)
    private val usdBaselineRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.9231,
        "GBP" to 0.7854,
        "JPY" to 155.62,
        "AUD" to 1.5034,
        "CAD" to 1.3682,
        "CHF" to 0.8941,
        "CNY" to 7.2435,
        "INR" to 83.4250,
        "SGD" to 1.3456,
        "HKD" to 7.8090,
        "NZD" to 1.6212,
        "MXN" to 17.5250,
        "BRL" to 5.2540,
        "ZAR" to 18.5830,
        "AED" to 3.6725,
        "SEK" to 10.4250,
        "NOK" to 10.5840,
        "TRY" to 32.2530,
        "KRW" to 1370.50
    )

    // In-memory cache of last fetched exchange rates
    private val cachedRates = mutableMapOf<String, Map<String, Double>>()

    /**
     * Obtains the exchange rate from a base currency to a target currency.
     * Tries the live network API. Falls back automatically to offline baseline rates.
     */
    suspend fun getExchangeRate(fromCode: String, toCode: String): Double = withContext(Dispatchers.IO) {
        if (fromCode == toCode) return@withContext 1.0

        // If we have cached rates for this base, use them
        cachedRates[fromCode]?.get(toCode)?.let { return@withContext it }

        try {
            Log.d("CurrencyRepository", "Fetching exchange rates online for base $fromCode")
            val response = currencyApi.getLatestRates(fromCode)
            val fetchedRates = response.rates
            cachedRates[fromCode] = fetchedRates
            fetchedRates[toCode] ?: getOfflineRate(fromCode, toCode)
        } catch (e: Exception) {
            Log.e("CurrencyRepository", "Failed to fetch live rates, falling back to offline values: ${e.localizedMessage}")
            getOfflineRate(fromCode, toCode)
        }
    }

    /**
     * Compiles rates dictionary mapping other currencies relative to the target base.
     */
    suspend fun getAllRatesForBase(baseCode: String): Map<String, Double> = withContext(Dispatchers.IO) {
        cachedRates[baseCode]?.let { return@withContext it }

        try {
            val response = currencyApi.getLatestRates(baseCode)
            cachedRates[baseCode] = response.rates
            response.rates
        } catch (e: Exception) {
            Log.e("CurrencyRepository", "Failed to load live rates list, compiling offline values")
            val offlineRates = mutableMapOf<String, Double>()
            for (curr in supportedCurrencies) {
                if (curr.code != baseCode) {
                    offlineRates[curr.code] = getOfflineRate(baseCode, curr.code)
                }
            }
            offlineRates
        }
    }

    private fun getOfflineRate(fromCode: String, toCode: String): Double {
        val usdToFrom = usdBaselineRates[fromCode] ?: 1.0
        val usdToTo = usdBaselineRates[toCode] ?: 1.0
        // rate = target_rate / base_rate
        return usdToTo / usdToFrom
    }

    // Room Database Bridging
    val conversionHistory: Flow<List<ConversionLog>> = currencyDao.getHistory()
    val favorites: Flow<List<CurrencyFavorite>> = currencyDao.getFavorites()

    suspend fun logConversion(from: String, to: String, fromAmount: Double, toAmount: Double, rate: Double) {
        withContext(Dispatchers.IO) {
            val log = ConversionLog(
                fromCurrency = from,
                toCurrency = to,
                fromAmount = fromAmount,
                toAmount = toAmount,
                rate = rate
            )
            currencyDao.insertHistory(log)
        }
    }

    suspend fun deleteHistoryItem(log: ConversionLog) {
        withContext(Dispatchers.IO) {
            currencyDao.deleteHistory(log)
        }
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            currencyDao.clearHistory()
        }
    }

    suspend fun toggleFavorite(currencyCode: String, isFav: Boolean) {
        withContext(Dispatchers.IO) {
            if (isFav) {
                currencyDao.insertFavorite(CurrencyFavorite(currencyCode, true))
            } else {
                currencyDao.removeFavoriteByCode(currencyCode)
            }
        }
    }
}
