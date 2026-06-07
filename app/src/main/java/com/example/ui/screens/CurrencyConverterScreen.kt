package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.entity.ConversionLog
import com.example.data.repository.CurrencyInfo
import com.example.ui.viewmodel.CurrencyViewModel
import com.example.ui.viewmodel.RateItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    viewModel: CurrencyViewModel,
    modifier: Modifier = Modifier
) {
    val amountInput by viewModel.amountInput.collectAsState()
    val fromCurrency by viewModel.fromCurrency.collectAsState()
    val toCurrency by viewModel.toCurrency.collectAsState()
    val conversionRate by viewModel.conversionRate.collectAsState()
    val conversionResult by viewModel.conversionResult.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val liveRatesList by viewModel.liveRatesList.collectAsState()
    val historyLogs by viewModel.historyLogs.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Navigation and Picker States
    var activeTab by remember { mutableStateOf(0) } // 0: Converter, 1: Live Rates, 2: History Log
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var searchRateQuery by remember { mutableStateOf("") }
    var successToastMessage by remember { mutableStateOf<String?>(null) }

    // Flags mapping
    val currencyInfoMap = remember(viewModel.supportedCurrenciesList) {
        viewModel.supportedCurrenciesList.associateBy { it.code }
    }

    val fromInfo = currencyInfoMap[fromCurrency]
    val toInfo = currencyInfoMap[toCurrency]

    // Clear error automatically after 4 seconds
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(4000)
            viewModel.clearErrorMessage()
        }
    }

    // Success toast timer
    LaunchedEffect(successToastMessage) {
        if (successToastMessage != null) {
            delay(2500)
            successToastMessage = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Material 3 stylized bottom Navigation Tab Bar
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Converter") },
                    label = { Text("Converter") },
                    modifier = Modifier.testTag("tab_converter")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Live Rates") },
                    label = { Text("Rates") },
                    modifier = Modifier.testTag("tab_rates")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Log") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("tab_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Toast notifications
                AnimatedVisibility(
                    visible = successToastMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    successToastMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Error notifications
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    errorMessage?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 1. Dynamic High Density Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 12.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "LAST UPDATE: JUST NOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when (activeTab) {
                                0 -> "Currency"
                                1 -> "Market Trends"
                                else -> "History Log"
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Avatar sync button styled with M3 container
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { viewModel.refreshRates() }
                            .testTag("refresh_rates_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync latest rates",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = if (isRefreshing) Modifier.clip(CircleShape) else Modifier
                        )
                    }
                }

                // Sync status indicator
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRefreshing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshing) "Syncing Live Rates..." else "Offline Baseline Protected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Switch pages based on Selected Navigation State
                Crossfade(targetState = activeTab, label = "tab_crossfade") { tabIndex ->
                    when (tabIndex) {
                        0 -> { // CONVERTER DASHBOARD
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        // A. FROM CURRENCY PANEL (WHITE BG)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .clickable { showFromPicker = true }
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .testTag("from_currency_picker"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(fromInfo?.flag ?: "🇺🇸", fontSize = 24.sp)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = fromCurrency,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = fromInfo?.name ?: "US Dollar",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Embedded right input amount
                                            OutlinedTextField(
                                                value = amountInput,
                                                onValueChange = { viewModel.setAmountInput(it) },
                                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.End
                                                ),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Decimal,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                ),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent,
                                                    disabledBorderColor = Color.Transparent,
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent
                                                ),
                                                placeholder = {
                                                    Text("0.00", style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.End), modifier = Modifier.fillMaxWidth())
                                                },
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .testTag("amount_input_field")
                                            )
                                        }

                                        // B. SWAP ACTION INTERSECT
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(y = (-1).dp)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                    .clickable { viewModel.swapCurrencies() }
                                                    .testTag("swap_currency_button"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SwapVert,
                                                    contentDescription = "Swap currencies",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // C. TO CURRENCY PANEL (LAVENDER PILL BG)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .clickable { showToPicker = true }
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                                .testTag("to_currency_picker"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(toInfo?.flag ?: "🇪🇺", fontSize = 24.sp)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = toCurrency,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = toInfo?.name ?: "Euro",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }

                                            // Output converted field
                                            Text(
                                                text = if (conversionResult != null) String.format("%,.2f", conversionResult) else "0.00",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.testTag("conversion_result_text")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // D. BOTTOM DETAILS ROW (EXCHANGE RANGE & COMPACT LOG ACTION)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Exchange Rate",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (conversionRate != null) "1 $fromCurrency = ${String.format("%.4f", conversionRate)} $toCurrency" else "---",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // Trigger Log conversion with premium small M3 pill shape button
                                            Button(
                                                onClick = {
                                                    viewModel.saveActiveConversion()
                                                    successToastMessage = "Logged to history: $amountInput $fromCurrency ➔ ${if (conversionResult != null) String.format("%.2f", conversionResult) else ""} $toCurrency"
                                                },
                                                shape = RoundedCornerShape(20.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                                modifier = Modifier.testTag("log_conversion_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> { // ALL LIVE RATES SHEET
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Search bar for All Rates
                                OutlinedTextField(
                                    value = searchRateQuery,
                                    onValueChange = { searchRateQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .testTag("rates_search_bar"),
                                    placeholder = { Text("Search by code or currency name", fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchRateQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchRateQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "EXCHANGE RATES RELATIVE TO 1 $fromCurrency",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )

                                val filteredRates = remember(liveRatesList, searchRateQuery) {
                                    if (searchRateQuery.isEmpty()) {
                                        liveRatesList
                                    } else {
                                        liveRatesList.filter {
                                            it.info.code.contains(searchRateQuery, ignoreCase = true) ||
                                                    it.info.name.contains(searchRateQuery, ignoreCase = true)
                                        }
                                    }
                                }

                                if (filteredRates.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                "No currencies found matching search",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = filteredRates,
                                            key = { it.info.code }
                                        ) { item ->
                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (item.isFavorite) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("rate_item_${item.info.code}"),
                                                border = if (item.isFavorite) {
                                                    CardDefaults.outlinedCardBorder().copy(
                                                        brush = Brush.horizontalGradient(
                                                            listOf(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                                            )
                                                        )
                                                    )
                                                } else null
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(item.info.flag, fontSize = 26.sp)
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text(
                                                                item.info.code,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 16.sp
                                                            )
                                                            Text(
                                                                item.info.name,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${item.info.symbol}${String.format("%,.4f", item.value)}",
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 16.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            textAlign = TextAlign.End,
                                                            modifier = Modifier.padding(end = 12.dp)
                                                        )

                                                        // Star Toggle Favorite
                                                        IconButton(
                                                            onClick = { viewModel.toggleFavorite(item.info.code) },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .testTag("toggle_favorite_${item.info.code}")
                                                        ) {
                                                            Icon(
                                                                imageVector = if (item.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                                                                contentDescription = "Bookmark ${item.info.code}",
                                                                tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> { // HISTORICAL LOG SHEET
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "YOUR PERSISTED CONVERSIONS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )

                                    if (historyLogs.isNotEmpty()) {
                                        TextButton(
                                            onClick = { viewModel.clearHistory() },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.testTag("clear_all_history_button")
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear All")
                                        }
                                    }
                                }

                                if (historyLogs.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Inventory,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(54.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "No history logs recorded yet",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Tap LOG inside the converter to save records",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = historyLogs,
                                            key = { it.id }
                                        ) { log ->
                                            val logFromInfo = currencyInfoMap[log.fromCurrency]
                                            val logToInfo = currencyInfoMap[log.toCurrency]

                                            Card(
                                                shape = RoundedCornerShape(20.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.loadHistoryItem(log)
                                                        activeTab = 0
                                                        successToastMessage = "Loaded conversion from logs"
                                                    }
                                                    .testTag("history_item_${log.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "${logFromInfo?.flag ?: "🇺🇸"} -> ${logToInfo?.flag ?: "🇪🇺"}",
                                                                fontSize = 18.sp
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "${log.fromCurrency} to ${log.toCurrency}",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = "${logFromInfo?.symbol ?: ""}${String.format("%,.2f", log.fromAmount)} ➔ ${logToInfo?.symbol ?: ""}${String.format("%,.2f", log.toAmount)}",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Rate: 1 ${log.fromCurrency} = ${String.format("%.4f", log.rate)} ${log.toCurrency}",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteHistoryItem(log) },
                                                        modifier = Modifier.testTag("delete_log_${log.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.DeleteOutline,
                                                            contentDescription = "Delete conversion record",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modaldialog Custom list-pickers search-enabled
    if (showFromPicker) {
        CurrencySelectionDialog(
            title = "Convert From",
            currencies = viewModel.supportedCurrenciesList,
            selectedCode = fromCurrency,
            onCurrencySelected = {
                viewModel.setFromCurrency(it)
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false },
            testTagPrefix = "from"
        )
    }

    if (showToPicker) {
        CurrencySelectionDialog(
            title = "Convert To",
            currencies = viewModel.supportedCurrenciesList,
            selectedCode = toCurrency,
            onCurrencySelected = {
                viewModel.setToCurrency(it)
                showToPicker = false
            },
            onDismiss = { showToPicker = false },
            testTagPrefix = "to"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionDialog(
    title: String,
    currencies: List<CurrencyInfo>,
    selectedCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    testTagPrefix: String
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(currencies, searchQuery) {
        if (searchQuery.isEmpty()) {
            currencies
        } else {
            currencies.filter {
                it.code.contains(searchQuery, ignoreCase = true) ||
                        it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Picker")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search in Picker
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_picker_search"),
                    placeholder = { Text("Search currencies...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable List of currencies
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.code }) { currency ->
                        val isSelected = currency.code == selectedCode

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else Color.Transparent
                                )
                                .clickable { onCurrencySelected(currency.code) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("select_item_${testTagPrefix}_${currency.code}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currency.flag, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currency.code,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    currency.name,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
