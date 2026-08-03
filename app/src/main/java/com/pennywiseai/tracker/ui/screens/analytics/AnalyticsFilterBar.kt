package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.DateRangeUtils
import java.time.LocalDate

@Composable
fun AnalyticsFilterBar(
    selectedPeriod: TimePeriod,
    customDateRange: Pair<LocalDate, LocalDate>?,
    selectedBank: String?,
    banks: List<String>,
    bankCounts: Map<String, Int>,
    transactionTypeFilter: TransactionTypeFilter,
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onPeriodSelected: (TimePeriod) -> Unit,
    onCustomRangeClick: () -> Unit,
    onBankSelected: (String?) -> Unit,
    onTypeFilterSelected: (TransactionTypeFilter) -> Unit,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTypeRow by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeriodFilterPill(
                selectedPeriod = selectedPeriod,
                customDateRange = customDateRange,
                onPeriodSelected = onPeriodSelected,
                onCustomRangeClick = onCustomRangeClick
            )

            if (banks.isNotEmpty()) {
                BankFilterPill(
                    selectedBank = selectedBank,
                    banks = banks,
                    bankCounts = bankCounts,
                    onBankSelected = onBankSelected
                )
            }

            FilterChip(
                selected = transactionTypeFilter != TransactionTypeFilter.EXPENSE || showTypeRow,
                onClick = { showTypeRow = !showTypeRow },
                label = {
                    Text(
                        typeFilterLabel(transactionTypeFilter),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { typeFilterIcon(transactionTypeFilter) },
                trailingIcon = {
                    Icon(
                        if (showTypeRow) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )

            if (availableCurrencies.size > 1) {
                CurrencyFilterPill(
                    selectedCurrency = selectedCurrency,
                    currencies = availableCurrencies,
                    onCurrencySelected = onCurrencySelected
                )
            }
        }

        if (showTypeRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TransactionTypeFilter.entries.forEach { typeFilter ->
                    FilterChip(
                        selected = transactionTypeFilter == typeFilter,
                        onClick = {
                            onTypeFilterSelected(typeFilter)
                            showTypeRow = false
                        },
                        label = { Text(typeFilter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterPill(
    selectedPeriod: TimePeriod,
    customDateRange: Pair<LocalDate, LocalDate>?,
    onPeriodSelected: (TimePeriod) -> Unit,
    onCustomRangeClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selectedPeriod == TimePeriod.CUSTOM && customDateRange != null) {
        DateRangeUtils.formatDateRange(customDateRange) ?: selectedPeriod.label
    } else {
        selectedPeriod.label
    }

    Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingIcon = {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimePeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = { Text(period.label) },
                    onClick = {
                        expanded = false
                        if (period == TimePeriod.CUSTOM) onCustomRangeClick()
                        else onPeriodSelected(period)
                    },
                    leadingIcon = {
                        Icon(periodIcon(period), null, Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun BankFilterPill(
    selectedBank: String?,
    banks: List<String>,
    bankCounts: Map<String, Int>,
    onBankSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedBank ?: "All banks"
    val count = if (selectedBank == null) bankCounts.values.sum() else bankCounts[selectedBank] ?: 0

    Box {
        FilterChip(
            selected = selectedBank != null,
            onClick = { expanded = true },
            label = {
                Text(
                    if (count > 0) "$label ($count)" else label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(Icons.Default.AccountBalance, null, Modifier.size(16.dp))
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All banks (${bankCounts.values.sum()})") },
                onClick = {
                    expanded = false
                    onBankSelected(null)
                }
            )
            banks.forEach { bank ->
                DropdownMenuItem(
                    text = { Text("$bank (${bankCounts[bank] ?: 0})") },
                    onClick = {
                        expanded = false
                        onBankSelected(bank)
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrencyFilterPill(
    selectedCurrency: String,
    currencies: List<String>,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(selectedCurrency) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        expanded = false
                        onCurrencySelected(currency)
                    }
                )
            }
        }
    }
}

private fun periodIcon(period: TimePeriod) = when (period) {
    TimePeriod.TODAY, TimePeriod.YESTERDAY -> Icons.Default.Today
    TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK -> Icons.Default.DateRange
    TimePeriod.LAST_7_DAYS, TimePeriod.LAST_30_DAYS -> Icons.Default.History
    TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
    TimePeriod.LAST_3_MONTHS, TimePeriod.LAST_6_MONTHS -> Icons.Default.CalendarMonth
    TimePeriod.THIS_QUARTER, TimePeriod.LAST_QUARTER -> Icons.Default.Assessment
    TimePeriod.ALL -> Icons.Default.AllInclusive
    TimePeriod.CUSTOM -> Icons.Default.Tune
    else -> Icons.Default.CalendarToday
}

internal fun typeFilterLabel(filter: TransactionTypeFilter): String = when (filter) {
    TransactionTypeFilter.ALL -> "All Types"
    TransactionTypeFilter.INCOME -> "Income"
    TransactionTypeFilter.EXPENSE -> "Expense"
    TransactionTypeFilter.CREDIT -> "Credit"
    TransactionTypeFilter.TRANSFER -> "Transfer"
    TransactionTypeFilter.INVESTMENT -> "Investment"
}

@Composable
internal fun typeFilterIcon(filter: TransactionTypeFilter) = when (filter) {
    TransactionTypeFilter.INCOME -> Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp))
    TransactionTypeFilter.EXPENSE -> Icon(Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(16.dp))
    TransactionTypeFilter.CREDIT -> Icon(Icons.Default.CreditCard, null, Modifier.size(16.dp))
    TransactionTypeFilter.TRANSFER -> Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
    TransactionTypeFilter.INVESTMENT -> Icon(Icons.AutoMirrored.Filled.ShowChart, null, Modifier.size(16.dp))
    else -> null
}
