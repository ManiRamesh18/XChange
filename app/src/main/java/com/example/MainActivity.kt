package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.database.AppDatabase
import com.example.data.repository.CurrencyRepository
import com.example.network.CurrencyApi
import com.example.ui.screens.CurrencyConverterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CurrencyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge to edge rendering for beautiful notch/gesture support
        enableEdgeToEdge()

        // Initialize dependencies securely
        val database = AppDatabase.getDatabase(applicationContext)
        val api = CurrencyApi.create()
        val repository = CurrencyRepository(database.currencyDao(), api)

        // Instantiate ViewModel with the factory
        val viewModel: CurrencyViewModel by viewModels {
            CurrencyViewModel.Factory(repository)
        }

        setContent {
            MyApplicationTheme {
                CurrencyConverterScreen(viewModel = viewModel)
            }
        }
    }
}
