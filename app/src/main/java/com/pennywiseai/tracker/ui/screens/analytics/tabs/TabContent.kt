package com.pennywiseai.tracker.ui.screens.analytics.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.components.charts.*
import com.pennywiseai.tracker.ui.components.insights.*
import com.pennywiseai.tracker.ui.screens.analytics.*
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.background
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.time.LocalDate

@Composable
fun SnapshotTab(
    state: AnalyticsUiState,
    onCategoryClick: (CategoryData) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(
            horizontal = Spacing.md,
            vertical = Spacing.sm
        )
    ) {
        item {
            IncomeVsExpenseCard(
                totalIncome = state.totalIncome,
                totalExpense = state.totalExpense,
                netSavings = state.netSavings,
                savingsRate = state.netSavingsRate,
                currency = state.currency
            )
        }

        if (state.comparisonData != null) {
            item {
                val c = state.comparisonData
                val comparisons = listOf(
                    ComparisonItem(
                        label = "Spending",
                        current = c.currentPeriod.totalSpending.let {
                            if (it > BigDecimal.ZERO) it else state.totalExpense
                        },
                        previous = c.previousPeriod.totalSpending,
                        currentLabel = c.currentPeriod.label,
                        previousLabel = c.previousPeriod.label
                    ),
                    ComparisonItem(
                        label = "Income",
                        current = c.currentPeriod.totalIncome.let {
                            if (it > BigDecimal.ZERO) it else state.totalIncome
                        },
                        previous = c.previousPeriod.totalIncome
                    )
                )
                PeriodComparisonCard(
                    comparisons = comparisons,
                    currency = state.currency
                )
            }
        }

        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }

        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                SpendingDonutChart(
                    categories = state.categoryBreakdown,
                    currency = state.currency,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        if (state.categoryTrends.isNotEmpty()) {
            item {
                CategoryTrendsCard(
                    trends = state.categoryTrends,
                    currency = state.currency
                )
            }
        }

        if (state.topMerchants.isNotEmpty()) {
            item {
                MerchantHorizontalBars(
                    merchants = state.topMerchants,
                    currency = state.currency
                )
            }
        }

        if (state.bankComparison.isNotEmpty()) {
            item {
                BankComparisonCard(
                    banks = state.bankComparison,
                    currency = state.currency
                )
            }
        }

        if (state.anomalousTransactions.isNotEmpty()) {
            item {
                AnomalyCard(
                    anomalies = state.anomalousTransactions,
                    currency = state.currency
                )
            }
        }
    }
}

@Composable
fun TrendsTab(
    state: AnalyticsUiState,
    onUpdateBudget: (Float, String?) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(
            horizontal = Spacing.md,
            vertical = Spacing.sm
        )
    ) {
        item {
            BudgetAlertCard(
                state = state.budgetState,
                currency = state.currency,
                banks = state.banks,
                onUpdateBudget = onUpdateBudget
            )
        }

        if (state.dailyTrend.isNotEmpty()) {
            item {
                DailyBarChart(
                    dailyData = state.dailyTrend.map {
                        DailyBarItem(
                            label = it.date.format(DateTimeFormatter.ofPattern("d MMM")),
                            amount = it.amount,
                            date = it.date
                        )
                    },
                    currency = state.currency,
                    title = "Daily Spending"
                )
            }
        }

        if (state.weeklyBreakdown.isNotEmpty()) {
            item {
                DailyBarChart(
                    dailyData = state.weeklyBreakdown.map {
                        DailyBarItem(
                            label = it.weekStart.format(DateTimeFormatter.ofPattern("d MMM")),
                            amount = it.amount
                        )
                    },
                    currency = state.currency,
                    title = "Weekly Spending"
                )
            }
        }

        if (state.monthlyBreakdown.isNotEmpty()) {
            item {
                MonthlyBarChart(
                    monthlyData = state.monthlyBreakdown.map {
                        MonthlyBarItem(
                            label = it.yearMonth.format(DateTimeFormatter.ofPattern("MMM yy")),
                            amount = it.amount,
                            income = it.income,
                            expenses = it.expenses,
                            yearMonth = it.yearMonth
                        )
                    },
                    currency = state.currency,
                    title = "Monthly Breakdown"
                )
            }
        }

        if (state.sixMonthSpending.isNotEmpty()) {
            item {
                SixMonthTrendCard(
                    monthlyData = state.sixMonthSpending,
                    currency = state.currency
                )
            }
        }

        if (state.heatmapDays.isNotEmpty()) {
            item {
                ActivityHeatmap(
                    dailyData = state.heatmapDays,
                    currency = state.currency
                )
            }
        }

        if (state.weekdayPattern.isNotEmpty()) {
            item {
                WeekdayPatternCard(
                    weekdayDistribution = state.weekdayPattern,
                    weekdayTotal = state.weekdaySpending,
                    weekendTotal = state.weekendSpending,
                    currency = state.currency
                )
            }
        }

        if (state.peakDays.isNotEmpty()) {
            item {
                PeakDaysCard(
                    peakDays = state.peakDays,
                    currency = state.currency
                )
            }
        }
    }
}

@Composable
fun BudgetAlertCard(
    state: BudgetUiState,
    currency: String,
    banks: List<String>,
    onUpdateBudget: (Float, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (state.limit <= 0f) {
        Card(
            onClick = { showDialog = true },
            modifier = modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Set a Monthly Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Track your burn rate and get predicted run-out warnings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Set Budget")
                }
            }
        }
    } else {
        val progress = if (state.limit > 0f) {
            (state.currentSpent / state.limit).coerceIn(0f, 1f)
        } else {
            0f
        }

        Card(
            modifier = modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isExceeded) Icons.Default.Warning else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (state.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.bank != null) "Monthly Budget (${state.bank})" else "Monthly Budget (All Banks)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (state.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent: ${CurrencyFormatter.formatCurrency(state.currentSpent.toDouble(), currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Budget: ${CurrencyFormatter.formatCurrency(state.limit.toDouble(), currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val messageText = remember(state) {
                    if (state.isExceeded) {
                        "⚠️ You have exceeded your monthly budget by ${CurrencyFormatter.formatCurrency((state.currentSpent - state.limit).toDouble(), currency)}!"
                    } else if (state.willExceed) {
                        val currentMonthName = LocalDate.now().month.name.lowercase().replaceFirstChar { it.titlecase() }
                        "⚠️ At your current rate of ${CurrencyFormatter.formatCurrency(state.dailyBurnRate.toDouble(), currency)}/day, you are projected to exceed your budget by ${CurrencyFormatter.formatCurrency((state.projectedSpending - state.limit).toDouble(), currency)} (around $currentMonthName ${state.exceedDayOfMonth})."
                    } else {
                        "✅ At your current rate of ${CurrencyFormatter.formatCurrency(state.dailyBurnRate.toDouble(), currency)}/day, you are on track to stay within your budget."
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (state.isExceeded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else if (state.willExceed) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isExceeded) MaterialTheme.colorScheme.onErrorContainer
                        else if (state.willExceed) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    if (showDialog) {
        EditBudgetDialog(
            currentLimit = state.limit,
            currentBank = state.bank,
            banks = banks,
            onDismiss = { showDialog = false },
            onConfirm = { limit, bank ->
                onUpdateBudget(limit, bank)
                showDialog = false
            }
        )
    }
}

@Composable
fun EditBudgetDialog(
    currentLimit: Float,
    currentBank: String?,
    banks: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Float, String?) -> Unit
) {
    var limitInput by remember { mutableStateOf(if (currentLimit > 0f) currentLimit.toInt().toString() else "") }
    var selectedBank by remember { mutableStateOf(currentBank) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Budget") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            limitInput = input
                        }
                    },
                    label = { Text("Monthly Budget Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Apply to Bank",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { bankDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = selectedBank ?: "All Banks")
                        }
                        DropdownMenu(
                            expanded = bankDropdownExpanded,
                            onDismissRequest = { bankDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Banks") },
                                onClick = {
                                    selectedBank = null
                                    bankDropdownExpanded = false
                                }
                            )
                            banks.forEach { bankName ->
                                DropdownMenuItem(
                                    text = { Text(bankName) },
                                    onClick = {
                                        selectedBank = bankName
                                        bankDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitInput.toFloatOrNull() ?: 0f
                    onConfirm(limit, selectedBank)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
