package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
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
import com.pennywiseai.tracker.ui.components.CustomDateRangePickerDialog
import com.pennywiseai.tracker.ui.screens.analytics.tabs.SnapshotTab
import com.pennywiseai.tracker.ui.screens.analytics.tabs.TrendsTab
import com.pennywiseai.tracker.ui.theme.*

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
    var showDateRangePicker by remember { mutableStateOf(false) }

    val tabs = remember { AnalyticsTab.entries }

    Column(modifier = Modifier.fillMaxSize()) {
        AnalyticsFilterBar(
            selectedPeriod = selectedPeriod,
            customDateRange = customDateRange,
            selectedBank = selectedBank,
            banks = uiState.banks,
            bankCounts = uiState.bankTransactionCounts,
            transactionTypeFilter = transactionTypeFilter,
            selectedCurrency = selectedCurrency,
            availableCurrencies = availableCurrencies,
            onPeriodSelected = { viewModel.selectPeriod(it) },
            onCustomRangeClick = { showDateRangePicker = true },
            onBankSelected = { viewModel.selectBank(it) },
            onTypeFilterSelected = { viewModel.setTransactionTypeFilter(it) },
            onCurrencySelected = { viewModel.selectCurrency(it) }
        )

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                // Always show hero + tabs once loaded so zero-spend periods still feel informative
                AnalyticsContent(
                    uiState = uiState,
                    selectedPeriod = selectedPeriod,
                    selectedCurrency = selectedCurrency,
                    activeTab = activeTab,
                    tabs = tabs,
                    showEmptyHint = uiState.transactionCount == 0,
                    onSelectTab = { viewModel.selectTab(it) },
                    onNavigateToTransactions = onNavigateToTransactions,
                    onUpdateBudget = { limit, bank -> viewModel.updateBudgetSettings(limit, bank) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDateRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                viewModel.setCustomDateRange(startDate, endDate)
                showDateRangePicker = false
            },
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second
        )
    }
}

@Composable
private fun AnalyticsContent(
    uiState: AnalyticsUiState,
    selectedPeriod: TimePeriod,
    selectedCurrency: String,
    activeTab: AnalyticsTab,
    tabs: List<AnalyticsTab>,
    showEmptyHint: Boolean,
    onSelectTab: (AnalyticsTab) -> Unit,
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit,
    onUpdateBudget: (Float, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        AnalyticsHero(
            state = uiState,
            selectedPeriod = selectedPeriod,
            onTodayClick = {
                onNavigateToTransactions(null, null, TimePeriod.TODAY.name, selectedCurrency)
            },
            onPeriodTotalClick = {
                onNavigateToTransactions(null, null, selectedPeriod.name, selectedCurrency)
            }
        )

        TabRow(
            selectedTabIndex = tabs.indexOf(activeTab).coerceAtLeast(0),
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
                    icon = {
                        Icon(tabIcon(tab), contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (showEmptyHint) {
                EmptyAnalyticsState()
            } else {
                when (activeTab) {
                    AnalyticsTab.SNAPSHOT -> SnapshotTab(
                        state = uiState,
                        onCategoryClick = {
                            onNavigateToTransactions(it.name, null, selectedPeriod.name, selectedCurrency)
                        }
                    )
                    AnalyticsTab.TRENDS -> TrendsTab(
                        state = uiState,
                        onUpdateBudget = onUpdateBudget
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimensions.Padding.empty)
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                "No data available",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Start tracking expenses to see analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun tabIcon(tab: AnalyticsTab) = when (tab) {
    AnalyticsTab.SNAPSHOT -> Icons.Default.Dashboard
    AnalyticsTab.TRENDS -> Icons.AutoMirrored.Filled.TrendingUp
}
