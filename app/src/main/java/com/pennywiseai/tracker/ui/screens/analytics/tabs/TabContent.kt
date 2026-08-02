package com.pennywiseai.tracker.ui.screens.analytics.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.components.charts.*
import com.pennywiseai.tracker.ui.components.insights.*
import com.pennywiseai.tracker.ui.screens.analytics.*
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.utils.DateRangeUtils
import java.time.LocalDate

@Composable
fun PeriodSelectorDropdownCard(
    selectedPeriod: TimePeriod,
    customDateRange: Pair<LocalDate, LocalDate>?,
    allowedPeriods: List<TimePeriod>,
    onPeriodSelected: (TimePeriod) -> Unit,
    onCustomRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selectedPeriod == TimePeriod.CUSTOM && customDateRange != null) {
        DateRangeUtils.formatDateRange(customDateRange) ?: selectedPeriod.label
    } else {
        selectedPeriod.label
    }

    Box(modifier = modifier) {
        Card(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Analysis Period",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            allowedPeriods.forEach { period ->
                DropdownMenuItem(
                    text = { Text(period.label) },
                    onClick = {
                        expanded = false
                        if (period == TimePeriod.CUSTOM) {
                            onCustomRangeClick()
                        } else {
                            onPeriodSelected(period)
                        }
                    },
                    leadingIcon = {
                        val icon = when (period) {
                            TimePeriod.TODAY, TimePeriod.YESTERDAY -> Icons.Default.Today
                            TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK -> Icons.Default.DateRange
                            TimePeriod.LAST_7_DAYS, TimePeriod.LAST_30_DAYS -> Icons.Default.History
                            TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH, TimePeriod.LAST_3_MONTHS, TimePeriod.LAST_6_MONTHS -> Icons.Default.CalendarMonth
                            TimePeriod.THIS_QUARTER, TimePeriod.LAST_QUARTER -> Icons.Default.Assessment
                            TimePeriod.ALL -> Icons.Default.AllInclusive
                            TimePeriod.CUSTOM -> Icons.Default.Tune
                            else -> Icons.Default.CalendarToday
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}

@Composable
fun OverviewTab(
    state: AnalyticsUiState,
    selectedPeriod: TimePeriod,
    customDateRange: Pair<LocalDate, LocalDate>?,
    onPeriodSelected: (TimePeriod) -> Unit,
    onCustomRangeClick: () -> Unit,
    onCategoryClick: (CategoryData) -> Unit = {},
    onMerchantClick: (MerchantData) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        item {
            PeriodSelectorDropdownCard(
                selectedPeriod = selectedPeriod,
                customDateRange = customDateRange,
                allowedPeriods = listOf(
                    TimePeriod.THIS_MONTH,
                    TimePeriod.LAST_MONTH,
                    TimePeriod.LAST_3_MONTHS,
                    TimePeriod.LAST_6_MONTHS,
                    TimePeriod.ALL,
                    TimePeriod.CUSTOM
                ),
                onPeriodSelected = onPeriodSelected,
                onCustomRangeClick = onCustomRangeClick
            )
        }

        // Summary
        item {
            IncomeVsExpenseCard(
                totalIncome = state.totalIncome,
                totalExpense = state.totalExpense,
                netSavings = state.netSavings,
                savingsRate = state.netSavingsRate,
                currency = state.currency
            )
        }

        // Period comparison
        if (state.comparisonData != null) {
            item {
                val c = state.comparisonData
                val totalExpense = state.totalExpense
                val comparisons = listOf(
                    ComparisonItem(
                        label = "Spending",
                        current = c.currentPeriod.totalSpending.let { if (it > BigDecimal.ZERO) it else totalExpense },
                        previous = c.previousPeriod.totalSpending.let { if (it > BigDecimal.ZERO) it else BigDecimal.ZERO },
                        currentLabel = c.currentPeriod.label,
                        previousLabel = c.previousPeriod.label
                    ),
                    ComparisonItem(
                        label = "Income",
                        current = c.currentPeriod.totalIncome.let { if (it > BigDecimal.ZERO) it else state.totalIncome },
                        previous = c.previousPeriod.totalIncome.let { if (it > BigDecimal.ZERO) it else BigDecimal.ZERO }
                    )
                )
                PeriodComparisonCard(
                    comparisons = comparisons,
                    currency = state.currency
                )
            }
        }

        // Spending velocity
        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }

        // 6-Month Spending Trend
        if (state.sixMonthSpending.isNotEmpty()) {
            item {
                SixMonthTrendCard(
                    monthlyData = state.sixMonthSpending,
                    currency = state.currency
                )
            }
        }

        // Activity heatmap (expense / income / both)
        if (state.heatmapDays.isNotEmpty()) {
            item {
                ActivityHeatmap(
                    dailyData = state.heatmapDays,
                    currency = state.currency
                )
            }
        }

        // Category donut
        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                SpendingDonutChart(
                    categories = state.categoryBreakdown,
                    currency = state.currency,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        // Weekday pattern
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

        // Category trends
        if (state.categoryTrends.isNotEmpty()) {
            item {
                CategoryTrendsCard(
                    trends = state.categoryTrends,
                    currency = state.currency
                )
            }
        }

        // Top merchants
        if (state.topMerchants.isNotEmpty()) {
            item {
                MerchantHorizontalBars(
                    merchants = state.topMerchants,
                    currency = state.currency
                )
            }
        }

        // Bank comparison
        if (state.bankComparison.isNotEmpty()) {
            item {
                BankComparisonCard(
                    banks = state.bankComparison,
                    currency = state.currency
                )
            }
        }

        // Anomaly detection
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
fun DailyTab(
    state: AnalyticsUiState,
    selectedPeriod: TimePeriod,
    customDateRange: Pair<LocalDate, LocalDate>?,
    onPeriodSelected: (TimePeriod) -> Unit,
    onCustomRangeClick: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        item {
            PeriodSelectorDropdownCard(
                selectedPeriod = selectedPeriod,
                customDateRange = customDateRange,
                allowedPeriods = listOf(
                    TimePeriod.TODAY,
                    TimePeriod.YESTERDAY,
                    TimePeriod.THIS_WEEK,
                    TimePeriod.LAST_WEEK,
                    TimePeriod.LAST_7_DAYS,
                    TimePeriod.LAST_30_DAYS,
                    TimePeriod.CUSTOM
                ),
                onPeriodSelected = onPeriodSelected,
                onCustomRangeClick = onCustomRangeClick
            )
        }

        // Daily bar chart
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

        // Weekday pattern
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

        // Peak days
        if (state.peakDays.isNotEmpty()) {
            item {
                PeakDaysCard(
                    peakDays = state.peakDays,
                    currency = state.currency
                )
            }
        }

        // Spending velocity
        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }
    }
}

@Composable
fun TrendsTab(
    state: AnalyticsUiState,
    selectedPeriod: TimePeriod,
    customDateRange: Pair<LocalDate, LocalDate>?,
    onPeriodSelected: (TimePeriod) -> Unit,
    onCustomRangeClick: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        item {
            PeriodSelectorDropdownCard(
                selectedPeriod = selectedPeriod,
                customDateRange = customDateRange,
                allowedPeriods = listOf(
                    TimePeriod.LAST_MONTH,
                    TimePeriod.LAST_3_MONTHS,
                    TimePeriod.LAST_6_MONTHS,
                    TimePeriod.THIS_QUARTER,
                    TimePeriod.LAST_QUARTER,
                    TimePeriod.CURRENT_FY,
                    TimePeriod.LAST_YEAR,
                    TimePeriod.ALL,
                    TimePeriod.CUSTOM
                ),
                onPeriodSelected = onPeriodSelected,
                onCustomRangeClick = onCustomRangeClick
            )
        }

        // Weekly breakdown bars
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

        // Monthly bar chart
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

        // Weekday pattern
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

        // Spending velocity
        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }
    }
}
