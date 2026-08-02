package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.components.CustomDateRangePickerDialog
import com.pennywiseai.tracker.ui.screens.analytics.tabs.*
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.DateRangeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val selectedBank by viewModel.selectedBank.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val timePeriods = remember { TimePeriod.entries }
    val customRangeLabel = remember(customDateRange) { DateRangeUtils.formatDateRange(customDateRange) }
    val tabs = remember { AnalyticsTab.entries }

    Column(modifier = Modifier.fillMaxSize()) {
        // Bank Tabs (now thin and elegant at the very top of filters)
        if (uiState.banks.isNotEmpty()) {
            AnalyticsBankTabs(
                banks = uiState.banks, selectedBank = selectedBank,
                counts = uiState.bankTransactionCounts, onSelect = { viewModel.selectBank(it) }
            )
        }

        // Currency + Type Filters Row
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (availableCurrencies.size > 1) {
                CurrencyFilterChips(selectedCurrency, availableCurrencies) { viewModel.selectCurrency(it) }
            }
            Spacer(Modifier.weight(1f))
            FilterChip(
                selected = transactionTypeFilter != TransactionTypeFilter.EXPENSE,
                onClick = { showAdvancedFilters = !showAdvancedFilters },
                label = { Text(typeFilterLabel(transactionTypeFilter)) },
                leadingIcon = { typeFilterIcon(transactionTypeFilter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }

        // Expanded Type Filters
        if (showAdvancedFilters) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(TransactionTypeFilter.entries) { typeFilter ->
                    FilterChip(
                        selected = transactionTypeFilter == typeFilter,
                        onClick = { viewModel.setTransactionTypeFilter(typeFilter) },
                        label = { Text(typeFilter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
        }

        // Tab Row (Fill width since there are only 3 tabs now)
        TabRow(
            selectedTabIndex = tabs.indexOf(activeTab).coerceAtLeast(0),
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = activeTab == tab, onClick = { viewModel.selectTab(tab) },
                    text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(tabIcon(tab), null, Modifier.size(16.dp)) }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.transactionCount == 0 -> EmptyAnalyticsState()
                else -> {
                    when (activeTab) {
                        AnalyticsTab.OVERVIEW -> OverviewTab(
                            state = uiState,
                            selectedPeriod = selectedPeriod,
                            customDateRange = customDateRange,
                            onPeriodSelected = { viewModel.selectPeriod(it) },
                            onCustomRangeClick = { showDateRangePicker = true },
                            onCategoryClick = { onNavigateToTransactions(it.name, null, selectedPeriod.name, selectedCurrency) },
                            onMerchantClick = { onNavigateToTransactions(null, it.name, selectedPeriod.name, selectedCurrency) }
                        )
                        AnalyticsTab.DAILY -> DailyTab(
                            state = uiState,
                            selectedPeriod = selectedPeriod,
                            customDateRange = customDateRange,
                            onPeriodSelected = { viewModel.selectPeriod(it) },
                            onCustomRangeClick = { showDateRangePicker = true }
                        )
                        AnalyticsTab.TRENDS -> TrendsTab(
                            state = uiState,
                            selectedPeriod = selectedPeriod,
                            customDateRange = customDateRange,
                            onPeriodSelected = { viewModel.selectPeriod(it) },
                            onCustomRangeClick = { showDateRangePicker = true }
                        )
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate -> viewModel.setCustomDateRange(startDate, endDate); showDateRangePicker = false },
            initialStartDate = customDateRange?.first, initialEndDate = customDateRange?.second
        )
    }
}

@Composable
private fun CurrencyFilterChips(cur: String, currencies: List<String>, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        items(currencies) { c ->
            FilterChip(
                selected = cur == c, onClick = { onSelect(c) },
                label = { Text(c, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun EmptyAnalyticsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Dimensions.Padding.empty)) {
            Icon(Icons.Default.Analytics, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(Spacing.md))
            Text("No data available", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(Spacing.sm))
            Text("Start tracking expenses to see analytics", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun AnalyticsBankTabs(banks: List<String>, selectedBank: String?, counts: Map<String, Int>, onSelect: (String?) -> Unit) {
    val tabs = listOf("All") + banks
    val selectedIndex = (selectedBank?.let { tabs.indexOf(it) } ?: 0).coerceAtLeast(0)
    ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp) {
        tabs.forEachIndexed { index, title ->
            val sel = index == selectedIndex
            Tab(selected = sel, onClick = { onSelect(if (index == 0) null else title) }) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50),
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (index == 0) "*" else title.take(2).uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.labelLarge,
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    val count = if (index == 0) counts.values.sum() else counts[title] ?: 0
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.height(20.dp)) {
                            Box(Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun typeFilterLabel(filter: TransactionTypeFilter): String = when (filter) {
    TransactionTypeFilter.ALL -> "All Types"
    TransactionTypeFilter.INCOME -> "Income"
    TransactionTypeFilter.EXPENSE -> "Expense"
    TransactionTypeFilter.CREDIT -> "Credit"
    TransactionTypeFilter.TRANSFER -> "Transfer"
    TransactionTypeFilter.INVESTMENT -> "Investment"
}

@Composable
private fun typeFilterIcon(filter: TransactionTypeFilter) = when (filter) {
    TransactionTypeFilter.INCOME -> Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp))
    TransactionTypeFilter.EXPENSE -> Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(16.dp))
    TransactionTypeFilter.CREDIT -> Icon(Icons.Default.CreditCard, null, Modifier.size(16.dp))
    TransactionTypeFilter.TRANSFER -> Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
    TransactionTypeFilter.INVESTMENT -> Icon(Icons.AutoMirrored.Filled.ShowChart, null, Modifier.size(16.dp))
    else -> null
}

private fun tabIcon(tab: AnalyticsTab) = when (tab) {
    AnalyticsTab.OVERVIEW -> Icons.Default.Home
    AnalyticsTab.DAILY -> Icons.Default.CalendarViewDay
    AnalyticsTab.TRENDS -> Icons.AutoMirrored.Filled.TrendingUp
}
