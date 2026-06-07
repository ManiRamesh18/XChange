package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ConversionLog
import com.example.data.repository.CurrencyInfo
import com.example.data.repository.CurrencyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RateItem(
    val info: CurrencyInfo,
    val value: Double,
    val isFavorite: Boolean
)

class CurrencyViewModel(private val repository: CurrencyRepository) : ViewModel() {

    // Input States
    private val _amountInput = MutableStateFlow("1.00")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    private val _fromCurrency = MutableStateFlow("USD")
    val fromCurrency: StateFlow<String> = _fromCurrency.asStateFlow()

    private val _toCurrency = MutableStateFlow("EUR")
    val toCurrency: StateFlow<String> = _toCurrency.asStateFlow()

    // Calculated States
    private val _conversionRate = MutableStateFlow<Double?>(null)
    val conversionRate: StateFlow<Double?> = _conversionRate.asStateFlow()

    private val _conversionResult = MutableStateFlow<Double?>(null)
    val conversionResult: StateFlow<Double?> = _conversionResult.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val supportedCurrenciesList: List<CurrencyInfo> = repository.supportedCurrencies

    // Favorites cache mapped to standard set for fast lookup
    val favoriteCodes: StateFlow<Set<String>> = repository.favorites
        .map { favs -> favs.map { it.currencyCode }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Live Rates List relative to the active _fromCurrency
    private val _liveRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    
    val liveRatesList: StateFlow<List<RateItem>> = combine(_liveRates, favoriteCodes) { rates, favs ->
        repository.supportedCurrencies
            .filter { it.code != _fromCurrency.value }
            .map { info ->
                RateItem(
                    info = info,
                    value = rates[info.code] ?: 0.0,
                    isFavorite = favs.contains(info.code)
                )
            }
            .sortedWith(
                compareByDescending<RateItem> { it.isFavorite }
                    .thenBy { it.info.code }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historical Logs
    val historyLogs: StateFlow<List<ConversionLog>> = repository.conversionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Trigger conversion and live list update when baseline properties modify
        viewModelScope.launch {
            combine(_fromCurrency, _toCurrency, _amountInput) { from, to, amount ->
                Triple(from, to, amount)
            }.collect { (from, to, amount) ->
                calculateConversion(from, to, amount)
                loadAllLiveRates(from)
            }
        }
    }

    fun setAmountInput(input: String) {
        // Sanitize input: allow only digits and at most one decimal point
        val sanitized = input.filter { it.isDigit() || it == '.' }
        val dotCount = sanitized.count { it == '.' }
        if (dotCount <= 1) {
            _amountInput.value = sanitized
        }
    }

    fun setFromCurrency(code: String) {
        if (code != _fromCurrency.value) {
            _fromCurrency.value = code
            // If from is now the same as to, swap to keep them distinct
            if (code == _toCurrency.value) {
                _toCurrency.value = repository.supportedCurrencies.firstOrNull { it.code != code }?.code ?: "EUR"
            }
        }
    }

    fun setToCurrency(code: String) {
        if (code != _toCurrency.value) {
            _toCurrency.value = code
            // If to is now the same as from, swap to keep them distinct
            if (code == _fromCurrency.value) {
                _fromCurrency.value = repository.supportedCurrencies.firstOrNull { it.code != code }?.code ?: "USD"
            }
        }
    }

    fun swapCurrencies() {
        val oldFrom = _fromCurrency.value
        _fromCurrency.value = _toCurrency.value
        _toCurrency.value = oldFrom
    }

    fun refreshRates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                val from = _fromCurrency.value
                val to = _toCurrency.value
                val amount = _amountInput.value

                // Re-pull and bypass memory caches
                calculateConversion(from, to, amount)
                loadAllLiveRates(from)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to synchronize latest exchange rates"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun calculateConversion(from: String, to: String, amountString: String) {
        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _conversionRate.value = null
            _conversionResult.value = null
            return
        }

        try {
            val rate = repository.getExchangeRate(from, to)
            _conversionRate.value = rate
            _conversionResult.value = amount * rate
        } catch (e: Exception) {
            _errorMessage.value = "Unable to compute currency rate conversion"
        }
    }

    private suspend fun loadAllLiveRates(base: String) {
        try {
            val rates = repository.getAllRatesForBase(base)
            _liveRates.value = rates
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load dynamic rates list"
        }
    }

    // Persisted conversions
    fun saveActiveConversion() {
        val from = _fromCurrency.value
        val to = _toCurrency.value
        val amount = _amountInput.value.toDoubleOrNull() ?: return
        val result = _conversionResult.value ?: return
        val rate = _conversionRate.value ?: return

        viewModelScope.launch {
            repository.logConversion(from, to, amount, result, rate)
        }
    }

    fun deleteHistoryItem(log: ConversionLog) {
        viewModelScope.launch {
            repository.deleteHistoryItem(log)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun toggleFavorite(currencyCode: String) {
        viewModelScope.launch {
            val currentFavs = favoriteCodes.value
            val isCurrentlyFav = currentFavs.contains(currencyCode)
            repository.toggleFavorite(currencyCode, !isCurrentlyFav)
        }
    }

    fun loadHistoryItem(log: ConversionLog) {
        _amountInput.value = String.format("%.2f", log.fromAmount)
        _fromCurrency.value = log.fromCurrency
        _toCurrency.value = log.toCurrency
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    class Factory(private val repository: CurrencyRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CurrencyViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CurrencyViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
